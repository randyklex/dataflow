package com.github.randyklex.dataflow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

class BoundingState {
    final int boundedCapacity;

    int currentCount;

    int getCurrentCount() {
        return currentCount;
    }

    void reduceCount(int numItems) {
        currentCount -= numItems;
    }

    void decrementCount() {
        currentCount--;
    }

    void incrementCount() {
        currentCount++;
    }

    BoundingState(int boundedCapacity)
    {
        if (boundedCapacity <= 0)
            throw new IllegalArgumentException("bounded is only supported with positive values.");

        this.boundedCapacity = boundedCapacity;
    }

    boolean countIsLessThanBound()
    {
        return getCurrentCount() < boundedCapacity;
    }
}

class BoundingStateWithPostponed<TInput> extends BoundingState
{
    int outstandingTransfers;

    final QueuedMap<ISourceBlock<TInput>, DataflowMessageHeader> postponedMessages = new QueuedMap<>();

    QueuedMap<ISourceBlock<TInput>, DataflowMessageHeader> getPostponedMessages() {
        return postponedMessages;
    }

    BoundingStateWithPostponed(int boundedCapacity)
    {
        super(boundedCapacity);
    }

    // TODO: left out the debugger thing.
}

/**
 * Stated used only when bounding and when postponed messages and a task are stored.
 * @param <TInput> Specifies the type of input messages.
 */
class BoundingStateWithPostponedAndTask<TInput> extends BoundingStateWithPostponed<TInput> {
    private CompletableFuture<?> taskForInputProcessing;

    Future<?> getTaskForInputProcessing() {
        return taskForInputProcessing;
    }

    void setTaskForInputProcessing(CompletableFuture<?> task) {
        taskForInputProcessing = task;
    }

    BoundingStateWithPostponedAndTask(int boundedCapacity) {
        super(boundedCapacity);
    }
}

// TODO: implement other bounding state classes here.
