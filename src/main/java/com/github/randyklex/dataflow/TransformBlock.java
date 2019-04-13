package com.github.randyklex.dataflow;

import javax.xml.transform.Source;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class TransformBlock<TInput, TOutput> implements IPropagatorBlock<TInput, TOutput>, IReceivableSourceBlock<TOutput> {

    private TargetCore<TInput> target;
    private SourceCore<TOutput> source;
    private ReorderingBuffer<TOutput> reorderingBuffer;

//    public TransformBock(Function<TInput, TOutput> transform) {
//    }

    public TransformBlock(Function<TInput, TOutput> transformSync, Function<TInput, FutureTask<TOutput>> transformAsync, ExecutionDataflowBlockOptions dataflowBlockOptions) {
        if (transformSync == null && transformAsync == null) throw new IllegalArgumentException("transform");
        if (dataflowBlockOptions == null) throw new NullPointerException("dataflowBlockOptions");

        if (transformAsync != null) {
            throw new IllegalArgumentException("transformAsync not currently supported, must be null");
        }

        dataflowBlockOptions = dataflowBlockOptions.DefaultOrClone();

        BiConsumer<ISourceBlock<TOutput>, Integer> onItemsRemoved = null;
        if (dataflowBlockOptions.getBoundedCapacity() > 0)
            onItemsRemoved = (owningSource, count) -> ((TransformBlock<TInput, TOutput>) owningSource).target.changeBoundingCount(-count);

        source = new SourceCore<>(this, dataflowBlockOptions,
                owningSource -> ((TransformBlock<TInput, TOutput>) owningSource).target.complete(null, true),
                onItemsRemoved);

        if (dataflowBlockOptions.getSupportsParallelExecution() && dataflowBlockOptions.getEnsureOrdered()) {
            reorderingBuffer = new ReorderingBuffer<TOutput>(this,
                    (owningSource, message) -> ((TransformBlock<TInput, TOutput>) owningSource).source.addMessage(message));
        }


    }

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
