package com.github.randyklex.dataflow;

import java.nio.Buffer;
import java.util.AbstractMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A propagator block that provides support for unbounded and bounded FIFO buffers.
 *
 * @param <T> Specifies the type of the data buffered by this dataflow block.
 */
public class BufferBlock<T> implements IPropagatorBlock<T, T>, IReceivableSourceBlock<T> {
    // The core logic for the buffer block
    private final SourceCore<T> source;

    // The bounding state for when in bounding mode; null if not bounding;
    private final BoundingStateWithPostponedAndTask<T> boundingState;

    private boolean targetDecliningPermanently;

    private boolean targetCompletionReserved;

    private Object getIncomingLock() {
        return source;
    }

    public BufferBlock() {
        this(DataflowBlockOptions.Default);
    }

    public BufferBlock(DataflowBlockOptions dataflowBlockOptions) {
        if (dataflowBlockOptions == null) {
            throw new NullPointerException("dataflowBlockOptions");
        }

        dataflowBlockOptions = dataflowBlockOptions.DefaultOrClone();

        BiConsumer<ISourceBlock<T>, Integer> onItemsRemoved = null;
        if (dataflowBlockOptions.getBoundedCapacity() > 0) {
            onItemsRemoved = (owningSource, count) -> ((BufferBlock<T>) owningSource).onItemsRemoved(count);
            boundingState = new BoundingStateWithPostponedAndTask<>(dataflowBlockOptions.getBoundedCapacity());
        }

        source = new SourceCore<>(this, dataflowBlockOptions,
                (owningSource) -> ((BufferBlock<T>) owningSource).complete(),
                onItemsRemoved);

        // TODO (si): what to do about exceptions?
        source.getCompletion().thenRunAsync(() -> {
            fault(null);
        });

        // TODO (si)
        //Common.wireCancellationToComplete();
    }



    /**
     * Notifies the block that one or more items was removed from the queue.
     *
     * @param numItemsRemoved The number of items removed.
     */
    private void onItemsRemoved(int numItemsRemoved) {
        assert numItemsRemoved > 0 : "A positive number of items to remove is required.";
        Common.contractAssertMonitorStatus(getIncomingLock(), false);

        if (boundingState != null) {
            synchronized (getIncomingLock()) {
                assert boundingState.getCurrentCount() - numItemsRemoved >= 0 :
                        "It should be impossible to have a negative number of items";
                boundingState.reduceCount(numItemsRemoved);

                consumeAsyncIfNecessary();
                completeTargetIfPossible();
            }
        }
    }

    void consumeAsyncIfNecessary() {
        consumeAsyncIfNecessary(false);
    }

    /**
     * Called when postponed messages may need to be consumed.
     *
     * @param isReplacementReplica Whether this call is the continuation of a previous message loop.
     */
    void consumeAsyncIfNecessary(boolean isReplacementReplica) {
        Common.contractAssertMonitorStatus(getIncomingLock(), true);
        assert boundingState != null : "Must be in bounded mode";

        if (!targetDecliningPermanently &&
                boundingState.getTaskForInputProcessing() == null &&
                boundingState.getPostponedMessages().size() > 0 &&
                boundingState.countIsLessThanBound()) {
            CompletableFuture<?> fut = CompletableFuture.runAsync(this::consumeMessagesLoopCore);
        }
    }

    private void consumeMessagesLoopCore() {
        assert boundingState != null && boundingState.getTaskForInputProcessing() != null :
                "May only be called in bounded mode and when a task is in flight.";
        Common.contractAssertMonitorStatus(getIncomingLock(), false);

        try {
            int maxMessagesPerTask = source.getDataflowBlockOptions().getActualMaxMessagesPerTask();
            for (int i = 0; i < maxMessagesPerTask && consumeAndStoreOneMessageIfAvailable(); ++i) ;
        } catch (Exception exc) {
            completeCore(exc, true);
        } finally {
            synchronized (getIncomingLock()) {
                boundingState.setTaskForInputProcessing(null);
                consumeAsyncIfNecessary(true);
                completeTargetIfPossible();
            }
        }
    }

    /**
     * Retrieves one postponed message if there's room and if we can consume a postponed message.
     * Stores any consumed messages into the source half.
     *
     * @return true if a message could be consumed and stored; otherwise, false.
     */
    private boolean consumeAndStoreOneMessageIfAvailable() {
        assert boundingState != null && boundingState.getTaskForInputProcessing() != null :
                "May only be called in bounded mode and when a task is in flight.";
        Common.contractAssertMonitorStatus(getIncomingLock(), false);

        // loop through the postponed messages until we get one.
        while (true) {
            AbstractMap.SimpleEntry<ISourceBlock<T>, DataflowMessageHeader> sourceAndMessage;
            synchronized (getIncomingLock()) {
                if (targetDecliningPermanently)
                    return false;
                if (!boundingState.countIsLessThanBound())
                    return false;

                TryResult<AbstractMap.SimpleEntry<ISourceBlock<T>, DataflowMessageHeader>> result = boundingState.getPostponedMessages().tryPoll();
                if (!result.isSuccess())
                    return false;

                sourceAndMessage = result.getResult();
                boundingState.incrementCount();
            }

            boolean consumed = false;
            try {
                TryResult<T> consumedValue = sourceAndMessage.getKey().consumeMessage(sourceAndMessage.getValue(), this);
                consumed = consumedValue.isSuccess();
                if (consumed) {
                    source.addMessage(consumedValue.getResult());
                    return true;
                }
            } finally {
                if (!consumed) {
                    synchronized (getIncomingLock()) {
                        boundingState.decrementCount();
                    }
                }
            }
        }
    }

    private void completeCore(Exception exception, boolean storeExceptionEvenIfAlreadyCompleting) {
        completeCore(exception, storeExceptionEvenIfAlreadyCompleting, false);
    }

    private void completeCore(Exception exception, boolean storeExceptionEvenifAlreadyCompleting, boolean revertProcessingState) {
        assert storeExceptionEvenifAlreadyCompleting || !revertProcessingState : "Indicating dirty processing state may only come with storeExceptionEvenIfAlreadyCompleting==true.";

        synchronized (getIncomingLock()) {
            // TODO (si) : exception stuff

            if (revertProcessingState) {
                assert boundingState != null && boundingState.getTaskForInputProcessing() != null :
                        "The processing state must be dirty when revertProcessingState==true.";
                boundingState.setTaskForInputProcessing(null);
            }

            targetDecliningPermanently = true;
            completeTargetIfPossible();
        }
    }

    /**
     * Completes the target, notifying the source, once all completion conditions are met.
     */
    private void completeTargetIfPossible() {
        Common.contractAssertMonitorStatus(getIncomingLock(), true);

        if (targetDecliningPermanently &&
                !targetCompletionReserved &&
                (boundingState == null || boundingState.getTaskForInputProcessing() == null)) {
            targetCompletionReserved = true;

            if (boundingState != null && boundingState.getPostponedMessages().size() > 0) {
                CompletableFuture.runAsync(() -> {
                    List<Exception> exceptions = null;
                    if (boundingState != null) {
                        exceptions = Common.releaseAllPostponedMessages(this,
                                this.boundingState.getPostponedMessages());
                    }

                    if (exceptions != null) {
                        source.addExceptions(exceptions);
                    }
                });
            }
        } else {
            source.complete();
        }
    }

    @Override
    public TryResult<T> TryReceive(Predicate<T> filter) {
        //return source.TryReceive(filter);
        return null;
    }

    @Override
    public TryResult<List<T>> TryReceiveAll() {
        //return source.TryReceiveAll();
        return null;
    }

    @Override
    public AutoCloseable linkTo(ITargetBlock<T> target, DataflowLinkOptions linkOptions) {
        return source.LinkTo(target, linkOptions);
    }

    @Override
    public TryResult<T> consumeMessage(DataflowMessageHeader messageHeader, ITargetBlock<T> target) {
        return null;
    }

    @Override
    public boolean reserveMessage(DataflowMessageHeader messageHeader, ITargetBlock<T> target) {
        return false;
    }

    @Override
    public void releaseReservation(DataflowMessageHeader messageHeader, ITargetBlock<T> target) {

    }

    @Override
    public DataflowMessageStatus offerMessage(DataflowMessageHeader messageHeader, T messageValue, ISourceBlock<T> source, boolean consumeToAccept) {
        return null;
    }

    @Override
    public CompletableFuture<?> getCompletion() {
        return null;
    }

    @Override
    public void complete() {
        completeCore(null, false);
    }

    @Override
    public void fault(Exception exception) {
        if (exception == null)
            throw new NullPointerException("exception");

        completeCore(exception, false);
    }
}
