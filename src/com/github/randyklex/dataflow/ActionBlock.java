package com.github.randyklex.dataflow;

import java.util.AbstractMap;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

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

        if (options.getSingleProducerConstrained() &&
                options.getMaxDegreeOfParallelism() == 1 &&
                options.getBoundedCapacity() == DataflowBlockOptions.Unbounded)
        {
            spscTarget = new SpscTargetCore<TInput>(this, action, options);
            defaultTarget = null;
        }
        else
        {
            defaultTarget = new TargetCore<TInput>(this,
                    (messageWithId) -> ProcessMessage(action, messageWithId),
                    null,
                    options,
                    EnumSet.of(TargetCore.TargetCoreOptions.REPRESENTS_BLOCK_COMPLETION));
            spscTarget = null;

            // TODO: implement the "wire cancellation" stuff.
        }

        // TODO: skipped FEATURE_TRACING
    }

    public ActionBlock(Function<TInput, CompletableFuture> action)
    {
        this(action, ExecutionDataflowBlockOptions.Default);
    }

    public ActionBlock(Function<TInput, CompletableFuture> action, ExecutionDataflowBlockOptions options)
    {
        if (action == null)
            throw new IllegalArgumentException("action cannot be null.");

        if (options == null)
            throw new IllegalArgumentException("options cannot be null.");

        options = options.DefaultOrClone();

        spscTarget = null;
        defaultTarget = new TargetCore<>(this,
                messageWithId -> processMessageWithTask(action, messageWithId),
                null,
                options, EnumSet.of(TargetCore.TargetCoreOptions.REPRESENTS_BLOCK_COMPLETION, TargetCore.TargetCoreOptions.USES_ASYNC_COMPLETION));

        // TODO: implement the "wire cancellation" stuff.

        // TODO: skipped FEATURE_TRACING
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
    private void processMessageWithTask(Function<TInput, CompletableFuture> action, AbstractMap.SimpleEntry<TInput, Long> messageWithId)
    {
        Future task = null;
        Exception caughtException = null;
        try
        {
            task = action.apply(messageWithId.getKey());
        }
        catch (Exception exc)
        {
            caughtException = exc;
        }

        // TODO: implement the rest of the Async stuff.
    }

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

    // TODO: really implement this function. Don't know how java handles TaskCompletionSource like .NET
    public CompletableFuture<?> getCompletion()
    {
        //return defaultTarget != null ? defaultTarget.getCompletion() : spscTarget.getCompletion();
        return null;
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
