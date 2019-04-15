package com.github.randyklex.dataflow;

import java.util.AbstractMap;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    // A task representing the completion of the block
    private final CompletableFuture<?> completionTask = new CompletableFuture<>();

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

    // TODO: reevaluate the need for 'repeat' here in light of how it's used in the .NET version.
    private void processAsyncIfNecessarySlow(boolean repeat)
    {
        boolean messagesAvailableOrPostponed = !messages.isEmpty() || (!decliningPermanently && boundingState != null && boundingState.countIsLessThanBound() && (boundingState.postponedMessages.size() > 0));

        if (messagesAvailableOrPostponed && !getCanceledOrFaulted())
        {
            numberOfOutstandingOperations++;
            if (getUsesAsyncCompletion())
                numberOfOutstandingServiceTasks++;

            Executors.newCachedThreadPool().submit(() -> {
                processMessagesLoopCore();
            });
            // TODO: Left off the FEATURE_TRACING
        }
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

            // TODO: implement the faulted or canceled check in this while loop.
            while (numberOfMessagesProcessedByThisTask < maxMessagesPerTask)
            {
                AbstractMap.SimpleEntry<TInput, Long> transferMessageWithId;
                if (shouldAttemptPostponedTransfer)
                {
                    TryResult<AbstractMap.SimpleEntry<TInput, Long>> tryConsumeResult = tryConsumePostponedMessage(true);
                    if (tryConsumeResult.isSuccess()) {
                        transferMessageWithId = tryConsumeResult.getResult();
                        synchronized (getIncomingLock()) {
                            // TODO: should we put the assert back in here?
                            boundingState.outstandingTransfers--;
                            messages.add(transferMessageWithId);
                            ProcessAsyncIfNecessary();
                        }
                    }
                }

                if (getUsesAsyncCompletion())
                {
                    // TODO: implement this code.
                    break;
                }
                else
                {
                    TryResult<AbstractMap.SimpleEntry<TInput, Long>> tryResult = tryGetNextAvailableOrPostponedMessage();
                    if (!tryResult.isSuccess())
                        break;
                    else
                    {
                        messageWithId = tryResult.getResult();
                        // Try to keep the task alive only if Max DOP is 1.
                        if (dataflowBlockOptions.getMaxDegreeOfParallelism() != 1)
                            break;

                        if (numberOfMessagesProcessedSinceTheLastKeepAlive > Common.KEEP_ALIVE_NUMBER_OF_MESSAGES_THRESHOLD)
                            break;

                        if (keepAliveBanCounter > 0) {
                            keepAliveBanCounter--;
                            break;
                        }

                        numberOfMessagesProcessedSinceTheLastKeepAlive = 0;
                    }
                }

                numberOfMessagesProcessedByThisTask++;
                numberOfMessagesProcessedSinceTheLastKeepAlive++;

                callAction.accept(messageWithId);
            }

        }
        catch (Exception e)
        {
            System.out.println(e.toString());
        }
        finally
        {
            synchronized (getIncomingLock())
            {
                // We incremented numberOfOutstandingOperations before we launched this
                // task. So we must decrement it before exiting. NOTE: Each async task
                // additionally incremented it before starting and is responsible for
                // decrementing it prior to exiting.
                numberOfOutstandingOperations--;

                if (getUsesAsyncCompletion())
                {
                    assert numberOfOutstandingServiceTasks > 0;
                    numberOfOutstandingServiceTasks--;
                }

                // However, we may have given up early because we hit our own configured
                // processing limits rather than because we ran out of work to do. If that's
                // the case, make sure we spin up another task to keep going.
                ProcessAsyncIfNecessary(true);

                // If however we stopped because we ran out of work to do and we
                // know we'll never get more, then complete.
                completeBlockIfPossible();
            }
        }
    }

    // TODO: implement tryGetNextMessageForNewAsyncOperation.

    private TryResult<AbstractMap.SimpleEntry<TInput, Long>> tryGetNextAvailableOrPostponedMessage()
    {
        // First, try to get a message from our input buffer.
        TryResult<AbstractMap.SimpleEntry<TInput, Long>> result = messages.tryPoll();
        if (result.isSuccess())
            return result;
        else if (boundingState != null)
        {
            // if we can't, but if we have postponed messages due to bounding,
            // then try to consume one of these postponed messages. Since we are
            // not currently holding the lock, it is possible that the new
            // messages get queued up by the time we take the lock to manipulate
            // boundingState. So we have to double-check the input queue once we
            // take the lock before we consider postponed messages.
            result = tryConsumePostponedMessage(false);
            if (result.isSuccess())
                return result;
        }

        return new TryResult<>(false, null);
    }

    private TryResult<AbstractMap.SimpleEntry<TInput, Long>> tryConsumePostponedMessage(boolean forPostponementTransfer)
    {
        boolean countIncrementedExpectingToGetItem = false;
        long messageId = Common.INVALID_REORDERING_ID;

        while (true)
        {
            AbstractMap.SimpleEntry<ISourceBlock<TInput>, DataflowMessageHeader> element;
            synchronized (getIncomingLock())
            {
                if (decliningPermanently)
                    break;

                // New messages may have been queued up while we weren't holding the lock.
                // In particular, the input queue may have been filled up and messages may
                // have gotten postponed. If we process such a postponed message, we would
                // mess up the order. Therefore, we have to double-check the input queue first.
                TryResult<AbstractMap.SimpleEntry<TInput, Long>> result = messages.tryPoll();
                if (!forPostponementTransfer && result.isSuccess())
                    return result;

                // We can consume a message to process if there's one to process and also if
                // we have logical room within our bound for the message.
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
            }

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
        if (reorderingBuffer != null && messageId != Common.INVALID_REORDERING_ID)
            reorderingBuffer.ignoreItem(messageId);

        if (countIncrementedExpectingToGetItem)
            changeBoundingCount(-1);

        return new TryResult<>(false, null);
    }

    CompletableFuture<?> getCompletion() {
        //
        return completionTask;
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
