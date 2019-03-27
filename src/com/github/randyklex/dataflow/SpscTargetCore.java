package com.github.randyklex.dataflow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

final class SpscTargetCore<TInput> {
    private final ITargetBlock<TInput> owningTarget;
    private final SingleProducerSingleConsumerQueue<TInput> messages = new SingleProducerSingleConsumerQueue<>();
    private final ExecutionDataflowBlockOptions blockOptions;
    private final Consumer<TInput> action;

    // TODO: figure out volatile in java.
    private List<Exception> exceptions;

    // TODO: readup on java version of 'volatile'
    private boolean decliningPermanently;

    // TODO: readup on java version of 'volatile'
    private boolean completionReserved;

    // TODO: implement the Task and TaskCompletionSource
    private AtomicReference<CompletableFuture<Void>> activeConsumer;

    SpscTargetCore(ITargetBlock<TInput> owningTarget, Consumer<TInput> action, ExecutionDataflowBlockOptions dataflowBlockOptions)
    {
        assert owningTarget != null;
        assert action != null;
        assert dataflowBlockOptions != null;

        this.owningTarget = owningTarget;
        this.action = action;
        this.blockOptions = dataflowBlockOptions;

        activeConsumer = new AtomicReference<>();
    }

    boolean post(TInput messageValue)
    {
        if (decliningPermanently)
            return false;

        messages.add(messageValue);

        // TODO: implement the memory barrier

        if (activeConsumer.get() == null)
            scheduleConsumerIfNecessary(false);

        return true;
    }

    DataflowMessageStatus offerMessage(DataflowMessageHeader messageHeader, TInput messageValue, ISourceBlock<TInput> source, boolean consumeToAccept)
    {
        if (!consumeToAccept && post(messageValue))
            return DataflowMessageStatus.Accepted;
        else
            return offerMessageSlow(messageHeader, messageValue, source, consumeToAccept);
    }

    private DataflowMessageStatus offerMessageSlow(DataflowMessageHeader messageHeader, TInput messageValue, ISourceBlock<TInput> source, boolean consumeToAccept)
    {
        if (decliningPermanently)
            return DataflowMessageStatus.DecliningPermanently;

        if (!messageHeader.isValid())
            throw new IllegalArgumentException("message is not valid.");

        if (consumeToAccept)
        {
            if (source == null)
                throw new IllegalArgumentException("source cannot be null.");

            boolean consumed;
            TryResult<TInput> result = source.consumeMessage(messageHeader, owningTarget);
            if (!result.isSuccess())
                return DataflowMessageStatus.NotAvailable;
        }

        messages.add(messageValue);
        // TODO: add the memory barrier stuff
        if (activeConsumer.get() == null)
            scheduleConsumerIfNecessary(false);

        return DataflowMessageStatus.Accepted;
    }

    private void scheduleConsumerIfNecessary(boolean isReplica)
    {
        if (activeConsumer.get() == null)
        {
            // TODO: implement the common.getCreationOptionsForTask

            CompletableFuture<Void> newConsumer = new CompletableFuture<>();
            if (activeConsumer.compareAndSet(null, newConsumer))
            {
                // TODO: skipped the FEATURE_TRACING

                Executors.newCachedThreadPool().submit(() -> {
                    processMessagesLoopCore();
                    newConsumer.complete(null);
                });
            }
        }
    }

    private void processMessagesLoopCore()
    {
        // TODO: implement the assertions

        int messagesProcessed = 0;
        int maxMessagesToProcess = blockOptions.getActualMaxMessagesPerTask();

        boolean continueProcessing = true;

        while (continueProcessing)
        {
            continueProcessing = false;
            TInput nextMessage = null;
            try
            {
                TryResult<TInput> result = messages.tryPoll();
                while (exceptions == null && messagesProcessed < maxMessagesToProcess && result.isSuccess())
                {
                    nextMessage = result.getResult();
                    messagesProcessed++;
                    action.accept(nextMessage);

                    result = messages.tryPoll();
                }
            }
            catch (Exception exception)
            {
                // TODO: implement the exception handling
            }
            finally
            {
                if (messages.isEmpty() && exceptions == null && (messagesProcessed < maxMessagesToProcess))
                {
                    continueProcessing = true;
                }
                else
                {
                    boolean wasDecliningPermanently = decliningPermanently;
                    if ((wasDecliningPermanently && messages.isEmpty()) || exceptions != null)
                    {
                        if (!completionReserved)
                        {
                            completionReserved = true;
                            completeBlockOncePossible();
                        }
                    }
                    else
                    {
                        // TODO: implement this else condition
                        CompletableFuture<Void> previousConsumer = activeConsumer.getAndSet(null);
                        // TODO: implement the other assertion here
                        assert previousConsumer != null;

                        if (!messages.isEmpty() || (!wasDecliningPermanently && decliningPermanently) || exceptions != null)
                            scheduleConsumerIfNecessary(true);
                    }
                }
            }
        }
    }

    int getInputSize() { return messages.size(); }

    void complete(Exception exception)
    {
        if (!decliningPermanently)
        {
            if (exception != null)
                StoreException(exception);

            decliningPermanently = true;
            scheduleConsumerIfNecessary(false);
        }
    }

    private void StoreException(Exception exception)
    {
        // TODO: this is really weird to me.. I don't think we want to do this, but just stubbing in for now.
        if (exceptions == null)
            exceptions = new ArrayList<>();

        synchronized (exceptions)
        {
            exceptions.add(exception);
        }
    }

    private void completeBlockOncePossible()
    {
        assert completionReserved;

        TInput dumpedMessage;
        TryResult<TInput> result = messages.tryPoll();
        while (result.isSuccess())
        {
            result = messages.tryPoll();
        }

        if (exceptions != null)
        {
            Exception[] exceptions;
            synchronized (this.exceptions)
            {
                exceptions = (Exception[])this.exceptions.toArray();
            }
            // TODO: add the completionSource stuff here.
        }
        // TODO: add the completion source stuff here.

        // TODO: skipped the FEATURE_TRACING
    }

    // TODO: implement the Completion method

    ExecutionDataflowBlockOptions getDataflowBlockOptions() { return blockOptions; }

    // TODO: skipped the Debugger methods at the end of this class.

    // TODO: skipped the DebuggingInformation class
}
