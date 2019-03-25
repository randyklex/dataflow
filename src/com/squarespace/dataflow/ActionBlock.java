package com.squarespace.dataflow;

import java.util.AbstractMap;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ActionBlock<TInput> implements ITargetBlock<TInput> {
    private final TargetCore<TInput> defaultTarget;

    private final SpscTargetCore<TInput> spscTarget;

    public ActionBlock(Consumer<TInput> action)
    {
        this(action, ExecutionDataflowBlockOptions.Default);
    }

    public ActionBlock(Consumer<TInput> action, ExecutionDataflowBlockOptions options)
    {
        if (action == null)
            throw new IllegalArgumentException("action cannot be null.");

        if (options == null)
            throw new IllegalArgumentException("options cannot be null.");

        options = options.DefaultOrClone();

        Consumer<TInput> syncAction = action;
        // TODO: add the CancellationToken to this 'if' statement.
        if (syncAction != null &&
                options.getSingleProducerConstrained() &&
                options.getMaxDegreeOfParallelism() == 1 &&
                options.getBoundedCapacity() == DataflowBlockOptions.Unbounded)
        {
            spscTarget = new SpscTargetCore<TInput>(this, syncAction, options);
            defaultTarget = null;
        }
        else
        {
            if (syncAction != null)
            {
                defaultTarget = new TargetCore<TInput>(this,
                        (messageWithId) -> ProcessMessage(syncAction, messageWithId),
                        options,
                        EnumSet.of(TargetCore.TargetCoreOptions.REPRESENTS_BLOCK_COMPLETION));
                spscTarget = null;
            }
            // TODO: implement the async part

            // TODO: handle async cancellation requests by declining on the target.
        }

        // TODO: skipped FEATURE_TRACING

        //
    }

    private void ProcessMessage(Consumer<TInput> action, AbstractMap.SimpleEntry<TInput, Long> messageWithId)
    {
        try
        {
            action.accept(messageWithId.getKey());
        }
        catch (Exception exc)
        {
            // TODO: implement the cooperative cancellation
        }
        finally
        {
            if (defaultTarget.isBounded())
                defaultTarget.changeBoundingCount(-1);
        }
    }

    // TODO: implement ProcessMessageWithTask

    // TODO: IMplement AsyncCompleteProcessMessageWithTask

    public void complete()
    {
        if (defaultTarget != null)
            defaultTarget.complete(null, false);
        else
            spscTarget.complete(null);
    }

    public void fault(Exception exception)
    {
        if (exception == null)
            throw new IllegalArgumentException("exception cannot be null.");

        if (defaultTarget != null)
            defaultTarget.complete(exception, true);
        else
            spscTarget.complete(exception);
    }

    public CompletableFuture<?> getCompletion()
    {
        return defaultTarget != null ? defaultTarget.getCompletion() : spscTarget.getCompletion();
    }

    public boolean post(TInput item)
    {
        if (defaultTarget != null)
            return defaultTarget.offerMessage(common.SingleMessageHeader, item, null, false) == DataflowMessageStatus.Accepted;
        else
            return spscTarget.post(item);
    }

    public DataflowMessageStatus offerMessage(DataflowMessageHeader messageHeader, TInput messageValue, ISourceBlock<TInput> source, boolean consumeToAccept)
    {
        if (defaultTarget != null)
            return defaultTarget.offerMessage(messageHeader, messageValue, source, consumeToAccept);
        else
            return spscTarget.offerMessage(messageHeader, messageValue, source, consumeToAccept);
    }
}
