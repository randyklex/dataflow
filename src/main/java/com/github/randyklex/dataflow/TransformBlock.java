package com.github.randyklex.dataflow;

import javax.xml.transform.Source;
import java.util.AbstractMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class TransformBlock<TInput, TOutput> implements IPropagatorBlock<TInput, TOutput>, IReceivableSourceBlock<TOutput> {

    private final TargetCore<TInput> target;
    private final SourceCore<TOutput> source;
    private ReorderingBuffer<TOutput> reorderingBuffer;


    // TODO (si) : Add the asynchronous ctors

    public TransformBlock(Function<TInput, TOutput> transform) {
        this(transform, ExecutionDataflowBlockOptions.Default);
    }

    public TransformBlock(Function<TInput, TOutput> transform, ExecutionDataflowBlockOptions dataflowBlockOptions) {
        this(transform, null, dataflowBlockOptions);
    }

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
            reorderingBuffer = new ReorderingBuffer<>(this,
                    (owningSource, message) -> ((TransformBlock<TInput, TOutput>) owningSource).source.addMessage(message));
        }

        // TODO (si) : add async support to this constructor
        //if(transformSync != null) {
        target = new TargetCore<>(this,
                messageWithId -> processMessage(transformSync, messageWithId),
                reorderingBuffer, dataflowBlockOptions, EnumSet.of(TargetCore.TargetCoreOptions.NONE));
//        } else {
//        }

        // Link up the target half with the source half.
        //target.
        // TODO (si) : verify that .net's 'ContinueWith' is equivalent to java's 'thenRunAsync'
        // TODO (si) : Should specify an executor the async continuation to execute on?
        target.getCompletion().thenRunAsync(() -> {
            //TODO (si) : do something with exceptions
            source.complete();
        });

        source.getCompletion().thenRunAsync(() -> {
            // TODO (si): some sort of "fault" logic belongs here. Don't yet understand enough about exceptions in futures
        });


    }

    // TODO (si): what to use for "KeyValuePair" - second argument
    private void processMessage(Function<TInput, TOutput> transform, Map.Entry<TInput, Long> messageWithId) {
        TOutput outputItem = null;
        boolean itemIsValid = false;

        try {
            outputItem = transform.apply(messageWithId.getKey());
            itemIsValid = true;
        } catch (Exception exc) {
            // TODO (si) : what is exception is cancellation?
        } finally {
            if (!itemIsValid) {
                target.changeBoundingCount(-1);
            }

            if (reorderingBuffer == null) {
                if (itemIsValid) {
                    if (target.getDataflowBlockOptions().getMaxDegreeOfParallelism() == 1) {
                        source.addMessage(outputItem);
                    } else {
                        synchronized (source) {
                            source.addMessage(outputItem);
                        }
                    }
                }
            } else {
                reorderingBuffer.addItem(messageWithId.getValue(), outputItem, itemIsValid);
            }
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
        return target.offerMessage(messageHeader, messageValue, source, consumeToAccept);
    }

    @Override
    public CompletableFuture<?> getCompletion() {
        return source.getCompletion();
    }

    @Override
    public void complete() {
        target.complete(null, false);
    }

    @Override
    public void fault(Exception exception) {

    }

    public int getInputSize() {
        return target.getInputSize();
    }

    public int getOutputSize() {
        return source.getOutputSize();
    }
}
