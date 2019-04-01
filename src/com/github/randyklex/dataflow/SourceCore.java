package com.github.randyklex.dataflow;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class SourceCore<TOutput> {
    private final TargetRegistry<TOutput> targetRegistry;

    // TODO: Implement the taskCompletionSource like in .NET. See also getOutgoingLock()

    private final SingleProducerSingleConsumerQueue<TOutput> messages = new SingleProducerSingleConsumerQueue<>();

    private final ISourceBlock<TOutput> owningSource;

    private final DataflowBlockOptions dataflowBlockOptions;

    private final Consumer<ISourceBlock<TOutput>> completeAction;

    private final BiConsumer<ISourceBlock<TOutput>, Integer> itemsRemovedAction;

    private final FunctionThreeParameters<ISourceBlock<TOutput>, TOutput, List<TOutput>, Integer> itemCountFunction;

    private long nextMessageId = 1;

    private ITargetBlock<TOutput> nextMessageReservedFor;

    private boolean decliningPermanently;

    private boolean enableOffering;

    private boolean completionReserved;

    private List<Exception> exceptions;

    // TODO implement this lock to use a TaskCompletionSource like .NET
    private Object getOutgoingLock() { return messages;}

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
            throw new IllegalArgumentException("linkOptions cannot be null.");

        // TODO: do the async stuff with Task/CompletableFuture

        synchronized (getOutgoingLock())
        {
            if (!completionReserved)
            {
                targetRegistry.add(target, linkOptions);
                offerToTargets(target);
                return common.CreateUnlinker(getOutgoingLock(), targetRegistry, target);
            }
        }

        // TODO: implement the propagatecompletion stuff here

        return AutoCloseables.Nop;
    }

    DataflowBlockOptions getDataflowBlockOptions() { return dataflowBlockOptions;}

    private boolean offerToTargets()
    {
        return offerToTargets(null);
    }

    private boolean offerToTargets(ITargetBlock<TOutput> linkToTarget)
    {
        if (nextMessageReservedFor != null)
            return false;

        DataflowMessageHeader header = new DataflowMessageHeader(DataflowMessageHeader.DEFAULT_VALUE);
        TOutput message = null;
        boolean offerJustToLinkToTarget = false;

        // TODO: do the volatile stuff here.
        if (enableOffering)
        {
            if (linkToTarget == null)
                return false;
            else
                offerJustToLinkToTarget = true;
        }

        // TODO: do the peak stuff in messages.

        boolean messageWasAccepted = false;
        if (header.isValid())
        {
            if (offerJustToLinkToTarget)
            {
                TryResult<Boolean> result = offerMessageToTarget(header, message, linkToTarget);
            }
            else
            {
                TargetRegistry<TOutput>.LinkedTargetInfo cur = targetRegistry.getFirstTargetNode();
                while (cur != null)
                {
                    TargetRegistry<TOutput>.LinkedTargetInfo next = cur.Next;
                    TryResult<Boolean> result = offerMessageToTarget(header, message, cur.Target);
                    if (result.isSuccess())
                        break;

                    cur = next;
                }

                if (!messageWasAccepted)
                {
                    synchronized (getValueLock())
                    {
                        enableOffering = false;
                    }
                }
            }
        }

        if (messageWasAccepted)
        {
            synchronized (getValueLock())
            {
                TOutput dropped;
            }
        }

        return messageWasAccepted;
    }

    private TryResult<Boolean> offerMessageToTarget(DataflowMessageHeader header, TOutput message, ITargetBlock<TOutput> target)
    {
        DataflowMessageStatus offerResult = target.offerMessage(header, message, owningSource, false);

        if (offerResult == DataflowMessageStatus.Accepted)
        {
            targetRegistry.Remove(target, true);
            return new TryResult<>(true, true);
        }
        else if (offerResult == DataflowMessageStatus.DecliningPermanently)
        {
            targetRegistry.Remove(target);
        }
        else if (nextMessageReservedFor != null)
        {
            // message should not be offered to anyone else.
            return new TryResult<>(true, false);
        }

        // allow the message to be offered to someone else.
        return new TryResult<>(false, false);
    }


}
