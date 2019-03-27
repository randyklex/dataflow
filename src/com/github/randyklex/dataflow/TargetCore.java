package com.github.randyklex.dataflow;

import java.util.AbstractMap;
import java.util.EnumSet;
import java.util.function.Consumer;

public class TargetCore<TInput> {

    enum TargetCoreOptions
    {
        NONE, USES_ASYNC_COMPLETION, REPRESENTS_BLOCK_COMPLETION;
    }

    private final ITargetBlock<TInput> owningTarget;
    private final IProducerConsumerQueue<AbstractMap.SimpleEntry<TInput, Long>> messages;
    private final ExecutionDataflowBlockOptions dataflowBlockOptions;
    private final Consumer<AbstractMap.SimpleEntry<TInput, Long>> callAction;
    private final EnumSet<TargetCoreOptions> targetCoreOptions;
    private final BoundingStateWithPostponed<TInput> boundingState;
    private final IReorderingBuffer reorderingBuffer;

    private boolean decliningPermanently;
    private int numberOfOutstandingOperations;
    private int numberOfOutstandingServiceTasks;
    // TODO: convert this variable below to an optimized padded variable per the original source
    private long nextAvailableInputMessageId;
    private boolean completionReserved;
    private int keepAliveBanCounter;

    // TODO: implement the reordering buffer.
    TargetCore(ITargetBlock<TInput> owningTarget,
               Consumer<AbstractMap.SimpleEntry<TInput, Long>> callAction,
               IReorderingBuffer reorderingBuffer,
               ExecutionDataflowBlockOptions dataflowBlockOptions,
               EnumSet<TargetCoreOptions> targetCoreOptions)
    {
        if (owningTarget == null)
            throw new IllegalArgumentException("Core must be associated with a target block.");

        if (dataflowBlockOptions == null)
            throw new IllegalArgumentException("Options must be provided to configure the core.");

        this.owningTarget = owningTarget;
        this.callAction = callAction;
        this.reorderingBuffer = reorderingBuffer;
        this.dataflowBlockOptions = dataflowBlockOptions;
        this.targetCoreOptions = targetCoreOptions;

        if (dataflowBlockOptions.getMaxDegreeOfParallelism() == 1)
            messages = new SingleProducerSingleConsumerQueue<>();
        else
            messages = new MultiProducerMultiConsumerQueue<>();

        if (dataflowBlockOptions.getBoundedCapacity() != com.github.randyklex.dataflow.DataflowBlockOptions.Unbounded) {
            // TODO: throw exception based on original source.
            boundingState = new BoundingStateWithPostponed<>(dataflowBlockOptions.getBoundedCapacity());
        }
        else
            boundingState = null;
    }

    private Object getIncomingLock() { return messages;}

    void complete(Exception exception, boolean dropPendingMessages)
    {
        complete(exception, dropPendingMessages, false, false, false);
    }

    void complete(Exception exception,
                  boolean dropPendingMessages,
                  boolean storeExceptionEvenIfAlreadyCompleting,
                  boolean unwrapInnerExceptions,
                  boolean revertProcessingState)
    {
        synchronized (getIncomingLock())
        {
            if (exception != null && (!decliningPermanently || storeExceptionEvenIfAlreadyCompleting))
            {
                // TODO: add the exception.
            }

            if (dropPendingMessages)
            {
                TryResult<AbstractMap.SimpleEntry<TInput, Long>> result = messages.tryPoll();
                while (result.isSuccess())
                {
                    result = messages.tryPoll();
                }
            }

            if (revertProcessingState)
            {
                numberOfOutstandingOperations--;
                if (getUsesAsyncCompletion())
                    numberOfOutstandingServiceTasks--;
            }

            decliningPermanently = true;
            completeBlockIfPossible();
        }
    }

    DataflowMessageStatus offerMessage(DataflowMessageHeader messageHeader,
                                       TInput messageValue,
                                       ISourceBlock<TInput> source,
                                       boolean consumeToAccept)
    {
        if (!messageHeader.isValid())
            throw new IllegalArgumentException("message header is invalid.");

        if (source == null && consumeToAccept)
            throw new IllegalArgumentException("Can't consume from a null source.");

        synchronized (getIncomingLock()) {
            if (decliningPermanently) {
                completeBlockIfPossible();
                return DataflowMessageStatus.DecliningPermanently;
            }

            /*
             * We can directly accept the message if
             *   1) we are not bounding, OR
             *   2) we are bounding AND there is room available AND there are no postponed messages AND
             *      no messages are currently being transferred to the input queue.
             */
            if (boundingState == null || (boundingState.outstandingTransfers == 0 && boundingState.countIsLessThanBound() && boundingState.postponedMessages.size() == 0))
            {
                if (consumeToAccept)
                {
                    TryResult<TInput> result = source.consumeMessage(messageHeader, owningTarget);
                    if (!result.isSuccess())
                        return DataflowMessageStatus.NotAvailable;
                    else
                        messageValue = result.getResult();
                }

                long messageId = nextAvailableInputMessageId++;

                if (boundingState != null) boundingState.CurrentCount += 1;

                messages.add(new AbstractMap.SimpleEntry<>(messageValue, messageId));
                ProcessAsyncIfNecessary();
                return DataflowMessageStatus.Accepted;
            }
            else if (source != null)
            {
                boundingState.postponedMessages.put(source, messageHeader);
                ProcessAsyncIfNecessary();
                return DataflowMessageStatus.Postponed;
            }

            return DataflowMessageStatus.Declined;
        }
    }

    int getInputSize()
    {
        return messages.sizeSafe(getIncomingLock());
    }

    private boolean getUsesAsyncCompletion()
    {
        return (targetCoreOptions.contains(TargetCoreOptions.USES_ASYNC_COMPLETION));
    }

    private void ProcessAsyncIfNecessary()
    {
        ProcessAsyncIfNecessary(false);
    }

    private void ProcessAsyncIfNecessary(boolean repeat)
    {
        if (getHasRoomForMoreServiceTasks())
        {
            processAsyncIfNecessarySlow(repeat);
        }
    }

    /*
     * Gets whether there's room to launch more processing operations.
    */
    private boolean getHasRoomForMoreOperations()
    {
        return (numberOfOutstandingOperations - numberOfOutstandingServiceTasks) < dataflowBlockOptions.getActualMaxDegreeOfParallelism();
    }

    private boolean getHasRoomForMoreServiceTasks()
    {
        // TODO: implement assertions

        if (!getUsesAsyncCompletion())
        {
            return getHasRoomForMoreOperations();
        }
        else
        {
            return getHasRoomForMoreOperations() && numberOfOutstandingServiceTasks < dataflowBlockOptions.getActualMaxDegreeOfParallelism();
        }
    }

    private void processAsyncIfNecessary()
    {
        processAsyncIfNecessary(false);
    }

    private void processAsyncIfNecessary(boolean repeat)
    {
        if (getHasRoomForMoreServiceTasks())
        {
            processAsyncIfNecessarySlow(repeat);
        }
    }

    private void processAsyncIfNecessarySlow(boolean repeat)
    {

    }

    private void processMessagesLoopCore()
    {

        AbstractMap.SimpleEntry<TInput, Long> messageWithId = new AbstractMap.SimpleEntry<>(null, 0L);

        try {

            boolean useAsyncCompletion = getUsesAsyncCompletion();
            boolean shouldAttemptPostponedTransfer = boundingState != null && boundingState.boundedCapacity > 1;
            int numberOfMessagesProcessedByThisTask = 0;
            int numberOfMessagesProcessedSinceTheLastKeepAlive = 0;
            int maxMessagesPerTask = dataflowBlockOptions.getActualMaxMessagesPerTask();

            // TODO: implement the faulted or canceled.
            while (numberOfMessagesProcessedByThisTask < maxMessagesPerTask)
            {

            }

        }
        catch (Exception e)
        {

        }
    }

    private TryResult<AbstractMap.SimpleEntry<TInput, Long>> tryGetNextAvailableOrPostponedMessage()
    {
        TryResult<AbstractMap.SimpleEntry<TInput, Long>> result = messages.tryPoll();
        if (result.isSuccess())
            return result;
        else
        {
            result = tryConsumePostponedMessage(false);
            if (result.isSuccess())
                return result;
            else
            {
                return new TryResult<>(false, null);
            }
        }
    }

    private TryResult<AbstractMap.SimpleEntry<TInput, Long>> tryConsumePostponedMessage(boolean forPostponementTransfer)
    {
        boolean countIncrementedExpectingToGetItem = false;
        long messageId = common.INVALID_REORDERING_ID;

        while (true)
        {
            AbstractMap.SimpleEntry<ISourceBlock<TInput>, DataflowMessageHeader> element;
            synchronized (getIncomingLock())
            {
                if (decliningPermanently)
                    break;

                TryResult<AbstractMap.SimpleEntry<TInput, Long>> result = messages.tryPoll();
                if (result.isSuccess() && !forPostponementTransfer)
                    return result;

                TryResult<AbstractMap.SimpleEntry<ISourceBlock<TInput>, DataflowMessageHeader>> postponedResult = boundingState.postponedMessages.tryPoll();
                element = postponedResult.getResult();
                if (!boundingState.countIsLessThanBound() || !postponedResult.isSuccess())
                {
                    if (countIncrementedExpectingToGetItem)
                    {
                        countIncrementedExpectingToGetItem = false;
                        boundingState.CurrentCount -= 1;
                    }
                    break;
                }

                if (!countIncrementedExpectingToGetItem)
                {
                    countIncrementedExpectingToGetItem = true;
                    messageId = nextAvailableInputMessageId++;
                    boundingState.CurrentCount += 1;
                    if (forPostponementTransfer)
                    {
                        boundingState.outstandingTransfers++;
                    }
                }
            } // Must not call to a source while holding lock.

            TryResult<TInput> consumedMessageStatus = element.getKey().consumeMessage(element.getValue(), owningTarget);
            if (consumedMessageStatus.isSuccess())
            {
                return new TryResult<>(true, new AbstractMap.SimpleEntry<>(consumedMessageStatus.getResult(), messageId));
            }
            else
            {
                if (forPostponementTransfer)
                    boundingState.outstandingTransfers--;
            }
        }

        // we optimistically acquired a message ID for a message that, in the end, we never got.
        // So we need to let the reordering buffer (if one exists) know that it should not
        // expect an item with this ID. Otherwise it would stall forever.
        if (reorderingBuffer != null && messageId != common.INVALID_REORDERING_ID)
            reorderingBuffer.ignoreItem(messageId);

        if (countIncrementedExpectingToGetItem)
            changeBoundingCount(-1);

        return new TryResult<>(false, null);
    }

    /*
     * Get whether the target has had a cancellation requested or an exception has occurred.
     */
    private boolean getCanceledOrFaulted()
    {
        // TODO: implement the cancellationToken stuff
        return false;
    }

    private void completeBlockIfPossible()
    {
        // TODO: implement assertion
        boolean noMoreMessages = decliningPermanently && messages.isEmpty();
        if (noMoreMessages || getCanceledOrFaulted())
        {
            completeBlockIfPossibleSlow();
        }
    }

    private void completeBlockIfPossibleSlow()
    {

        boolean notCurrentlyProcessing = numberOfOutstandingOperations == 0;
        if (notCurrentlyProcessing && !completionReserved)
        {
            completionReserved = true;

            decliningPermanently = true;
        }
    }

    private void completeBlockOncePossible()
    {
        // TODO: impelment the assertion.

        if (boundingState != null)
        {
            // TODO: implement the releaseAllPostponedMessages
        }

        TryResult<AbstractMap.SimpleEntry<TInput, Long>> result = messages.tryPoll();
        while (result.isSuccess())
        {
            result = messages.tryPoll();
        }

        // TODO: implement the exception handling and cancellation stuff.

        // TODO: Skipped the FEATURE_TRACING
    }

    boolean isBounded() { return boundingState != null;}

    void changeBoundingCount(int count)
    {
        // TODO: implement the assertions
        if (boundingState != null)
        {
            synchronized (getIncomingLock())
            {
                boundingState.CurrentCount += count;
                processAsyncIfNecessary();
                completeBlockIfPossible();
            }
        }
    }

    ExecutionDataflowBlockOptions getDataflowBlockOptions() { return dataflowBlockOptions; }

    // TODO: skipped the debug stuff below here.
}
