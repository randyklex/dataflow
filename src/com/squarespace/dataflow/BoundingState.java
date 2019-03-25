package com.squarespace.dataflow;

class BoundingState {
    final int boundedCapacity;

    int CurrentCount;

    BoundingState(int boundedCapacity)
    {
        if (boundedCapacity <= 0)
            throw new IllegalArgumentException("bounded is only supported with positive values.");

        this.boundedCapacity = boundedCapacity;
    }

    boolean countIsLessThanBound()
    {
        return CurrentCount < boundedCapacity;
    }
}

class BoundingStateWithPostponed<TInput> extends BoundingState
{
    int outstandingTransfers;

    final QueuedMap<ISourceBlock<TInput>, DataflowMessageHeader> postponedMessages = new QueuedMap<ISourceBlock<TInput>, DataflowMessageHeader>();

    BoundingStateWithPostponed(int boundedCapacity)
    {
        super(boundedCapacity);
    }

    private int getPostponedMessagesCountForDebugger()
    {
        return postponedMessages.size();
    }
}

class BoundingStateWithPostponedAndTask<TInput> extends BoundingStateWithPostponed<TInput>
{
    BoundingStateWithPostponedAndTask(int boundedCapacity)
    {
        super(boundedCapacity);
    }
}
