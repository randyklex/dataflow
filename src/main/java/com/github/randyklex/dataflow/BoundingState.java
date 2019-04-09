package com.github.randyklex.dataflow;

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

    final QueuedMap<ISourceBlock<TInput>, DataflowMessageHeader> postponedMessages = new QueuedMap<>();

    BoundingStateWithPostponed(int boundedCapacity)
    {
        super(boundedCapacity);
    }

    // TODO: left out the debugger thing.
}

// TODO: implement other bounding state classes here.
