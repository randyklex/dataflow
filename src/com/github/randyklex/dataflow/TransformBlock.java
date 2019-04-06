package com.github.randyklex.dataflow;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class TransformBlock<TInput, TOutput> implements IPropagatorBlock<TInput, TOutput>, IReceivableSourceBlock<TOutput> {

    @Override
    public TryResult<TOutput> TryReceive(Predicate<TOutput> filter) {
        return null;
    }

    @Override
    public TryResult<List<TOutput>> TryReceiveAll() {
        return null;
    }

    @Override
    public AutoCloseable linkTo(ITargetBlock<TOutput> target) {
        return null;
    }

    @Override
    public AutoCloseable linkTo(ITargetBlock<TOutput> target, DataflowLinkOptions linkOptions) {
        return null;
    }

    @Override
    public AutoCloseable linkTo(ITargetBlock<TOutput> target, Predicate<TOutput> predicate) {
        return null;
    }

    @Override
    public AutoCloseable linkTo(ITargetBlock<TOutput> target, DataflowLinkOptions linkOptions, Predicate<TOutput> predicate) {
        return null;
    }

    @Override
    public TryResult<TOutput> consumeMessage(DataflowMessageHeader messageHeader, ITargetBlock<TOutput> target) {
        return null;
    }

    @Override
    public boolean reserveMessage(DataflowMessageHeader messageHeader, ITargetBlock<TOutput> target) {
        return false;
    }

    @Override
    public void releaseReservation(DataflowMessageHeader messageHeader, ITargetBlock<TOutput> target) {

    }

    @Override
    public DataflowMessageStatus offerMessage(DataflowMessageHeader messageHeader, TInput messageValue, ISourceBlock<TInput> source, boolean consumeToAccept) {
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
