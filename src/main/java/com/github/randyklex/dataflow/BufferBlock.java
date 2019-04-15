package com.github.randyklex.dataflow;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class BufferBlock<T> implements IPropagatorBlock<T, T>, IReceivableSourceBlock<T> {
    @Override
    public TryResult<T> TryReceive(Predicate<T> filter) {
        return null;
    }

    @Override
    public TryResult<List<T>> TryReceiveAll() {
        return null;
    }

    @Override
    public AutoCloseable linkTo(ITargetBlock<T> target) {
        return null;
    }

    @Override
    public AutoCloseable linkTo(ITargetBlock<T> target, DataflowLinkOptions linkOptions) {
        return null;
    }

    @Override
    public AutoCloseable linkTo(ITargetBlock<T> target, Predicate<T> predicate) {
        return null;
    }

    @Override
    public AutoCloseable linkTo(ITargetBlock<T> target, DataflowLinkOptions linkOptions, Predicate<T> predicate) {
        return null;
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

    }

    @Override
    public void fault(Exception exception) {

    }
}
