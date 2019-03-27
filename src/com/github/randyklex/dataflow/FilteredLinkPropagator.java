package com.github.randyklex.dataflow;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public final class FilteredLinkPropagator<T> implements IPropagatorBlock<T, T> {
    private final ISourceBlock<T> source;
    private final ITargetBlock<T> target;
    private final Predicate<T> userProvidedPredicate;

    FilteredLinkPropagator(ISourceBlock<T> source, ITargetBlock<T> target, Predicate<T> predicate)
    {
        this.source = source;
        this.target = target;
        this.userProvidedPredicate = predicate;
    }

    private boolean RunPredicate(T item)
    {
        return userProvidedPredicate.test(item);
    }

    public DataflowMessageStatus offerMessage(DataflowMessageHeader messageHeader, T messageValue, ISourceBlock<T> source, boolean consumeToAccept)
    {
        if (!messageHeader.isValid())
            throw new IllegalArgumentException("messageHeader is invalid.");

        if (source == null)
            throw new IllegalArgumentException("source cannot be null.");

        boolean passedFilter = RunPredicate(messageValue);

        if (passedFilter)
            return this.target.offerMessage(messageHeader, messageValue, this, consumeToAccept);
        else
            return DataflowMessageStatus.Declined;
    }

    public TryResult<T> consumeMessage(DataflowMessageHeader messageHeader, ITargetBlock<T> targetBlock)
    {
        // TODO: convert this to a debug assertion?
        if (!messageHeader.isValid())
            throw new IllegalArgumentException("messageHeader is invalid.");

        return this.source.consumeMessage(messageHeader, targetBlock);
    }

    public boolean reserveMessage(DataflowMessageHeader messageHeader, ITargetBlock<T> targetBlock)
    {
        // TODO: convert this to a debug assertion?
        if (!messageHeader.isValid())
            throw new IllegalArgumentException("messageHeader is invalid.");

        return this.source.reserveMessage(messageHeader, targetBlock);
    }

    public void releaseReservation(DataflowMessageHeader messageHeader, ITargetBlock<T> targetBlock)
    {
        if (!messageHeader.isValid())
            throw new IllegalArgumentException("messageHeader is invalid.");

        this.source.releaseReservation(messageHeader, targetBlock);
    }

    public AutoCloseable linkTo(ITargetBlock<T> targetBlock, DataflowLinkOptions linkOptions)
    {
        throw new NotImplementedException();
    }

    public void complete()
    {
        this.target.complete();
    }

    public CompletableFuture<?> getCompletion()
    {
        return source.getCompletion();
    }

    public void fault(Exception exception)
    {
        this.target.fault(exception);
    }
}
