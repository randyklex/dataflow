package com.github.randyklex.dataflow;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class TransformBlock<TInput, TOutput> implements IPropagatorBlock<TInput, TOutput>, IReceivableSourceBlock<TOutput> {
    private final TargetCore<TInput> target;
    private final ReordingBuffer<TOutput> reordingBuffer;
    private final SourceCore<TOutput> source;

    private Object getParallelSourceLock()
    {
        return source;
    }

    public TransformBlock(Function<TInput, TOutput> transform)
    {

    }

    public TransformBlock(Function<TInput, TOutput>, ExecutionDataflowBlockOptions dataflowBlockOptions)
    {

    }

    private TransformBlock(Function<TInput, TOutput> transformSync,
                           Function<TInput, CompletableFuture<TOutput>> transformAsync,
                           ExecutionDataflowBlockOptions dataflowBlockOptions)
    {
        if (transformSync == null && transformAsync == null)
            throw new IllegalArgumentException("transform is required.");

        if (dataflowBlockOptions == null)
            throw new IllegalArgumentException("dataflowBlockOptions cannot be null.");

        dataflowBlockOptions = dataflowBlockOptions.DefaultOrClone();

        BiConsumer<ISourceBlock<TOutput>, Integer> onItemsRemoved = null;

        if (dataflowBlockOptions.getBoundedCapacity() > 0)
            onItemsRemoved = (owningSource, count) -> {((TransformBlock<TInput, TOutput>)owningSource).target.ChangeBoundingCount(-count);};

        source = new SourceCore<TOutput>(this, dataflowBlockOptions,
                owningSource -> {},
                onItemsRemoved);

        if (dataflowBlockOptions.getSupportsParallelExecution() && dataflowBlockOptions.getEnsureOrdered())
        {

        }

        if (transformSync != null)
        {
            target = new TargetCore<TInput>(this,
                    messageWithId -> ProcessMessage(transformSync, messageWithId),
                    reorderingBuffer,
                    dataflowBlockOptions,
                    TargetCore.TargetCoreOptions.NONE);
        }
        else
        {
            // TODO: do the async setup here.
        }
    }
}
