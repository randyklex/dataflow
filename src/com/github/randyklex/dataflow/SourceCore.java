package com.github.randyklex.dataflow;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class SourceCore<TOutput> {


    private final TargetRegistry<TOutput> targetRegistry;

    private final SingleProducerSingleConsumerQueue<TOutput> messages = new SingleProducerSingleConsumerQueue<>();

    private final ISourceBlock<TOutput> owningSource;

    private final DataflowBlockOptions dataflowBlockOptions;

    private final Consumer<ISourceBlock<TOutput>> completeAction;

    private final BiConsumer<ISourceBlock<TOutput>, Integer> itemsRemovedAction;

    private final FunctionThreeParameters<ISourceBlock<TOutput>, TOutput, List<TOutput>, Integer> itemCountFunction;

    private PaddedInt64 nextMessageId = new PaddedInt64(Value = 1);

    private ITargetBlock<TOutput> nextMessageReservedFor;

    private boolean decliningPermanently;

    private boolean enableOffering;

    private boolean completionReserved;

    private List<Exception> exceptions;

    private Object getOutgoingLock() { return completionTask;}

    private Object getValueLock() { return targetRegistry;}

    SourceCore(ISourceBlock<TOutput> owningSource,
               DataflowBlockOptions dataflowBlockOptions,
               Consumer<ISourceBlock<TOutput>> completeAction)
    {
        this(owningSource, dataflowBlockOptions, completeAction, null, null);
    }

    SourceCore(ISourceBlock<TOutput> owningSource,
               DataflowBlockOptions dataflowBlockOptions,
               Consumer<ISourceBlock<TOutput>> completeAction,
               BiConsumer<ISourceBlock<TOutput>, Integer> itemsRemovedAction,
               FunctionThreeParameters<ISourceBlock<TOutput>, TOutput, List<TOutput>, Integer> itemCountingFunction)
    {
        this.owningSource = owningSource;
        this.dataflowBlockOptions = dataflowBlockOptions;
        this.itemsRemovedAction = itemsRemovedAction;
        this.itemCountFunction = itemCountingFunction;
        this.completeAction = completeAction;

        this.targetRegistry = new TargetRegistry<TOutput>(owningSource);
    }

    AutoCloseable LinkTo(ITargetBlock<TOutput> target, DataflowLinkOptions linkOptions)
    {
        if (target == null)
            throw new IllegalArgumentException("target cannot be null");

        if (linkOptions == null)
            throw new IllegalArgumentException("linkoptions cannot be null.");

        synchronized (getOutgoingLock())
        {
            if (!completionReserved)
            {
                targetRegistry.add(target, linkOptions);
                OfferToTargets(target);
            }
        }
    }

    DataflowBlockOptions getDataflowBlockOptions() { return dataflowBlockOptions;}

    private boolean OfferToTargets()
    {
        return OfferToTargets(null);
    }

    private boolean OfferToTargets(ITargetBlock<TOutput> linkToTarget)
    {

        if (nextMessageReservedFor != null)
            return false;

        DataflowMessageHeader header = new DataflowMessageHeader();
        TOutput message = default(TOutput);
        boolean offerJustToLinkToTarget = false;


    }


}
