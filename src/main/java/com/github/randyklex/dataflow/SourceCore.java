package com.github.randyklex.dataflow;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class SourceCore<TOutput> {
    private final TargetRegistry<TOutput> targetRegistry;

    private final CompletableFuture<?> completionTask = new CompletableFuture<>();

    private final SingleProducerSingleConsumerQueue<TOutput> messages = new SingleProducerSingleConsumerQueue<>();

    private final ISourceBlock<TOutput> owningSource;

    private final DataflowBlockOptions dataflowBlockOptions;

    private final Consumer<ISourceBlock<TOutput>> completeAction;

    private final BiConsumer<ISourceBlock<TOutput>, Integer> itemsRemovedAction;

    private final FunctionThreeParameters<ISourceBlock<TOutput>, TOutput, List<TOutput>, Integer> itemCountFunction;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private long nextMessageId = 1;

    private ITargetBlock<TOutput> nextMessageReservedFor;

    private boolean decliningPermanently;

    private boolean enableOffering;

    private boolean completionReserved;

    private List<Exception> exceptions;


    // TODO: what is the correct type here?
    //private CompletableFuture<TOutput> taskForOutputProcessing;
    //private Future<?> taskForOutputProcessing;
    //private CompletableFuture<?> taskForOutputProcessing;
    private Future<?> taskForOutputProcessing;

    // TODO implement this lock to use a TaskCompletionSource like .NET
    private Object getOutgoingLock() {
        //return messages;
        return completionTask;
    }

    CompletableFuture<?> getCompletion() {
        return completionTask;
    }

    private Object getValueLock() {
        return targetRegistry;
    }

    SourceCore(ISourceBlock<TOutput> owningSource,
               DataflowBlockOptions dataflowBlockOptions,
               Consumer<ISourceBlock<TOutput>> completeAction) {
        this(owningSource, dataflowBlockOptions, completeAction, null, null);
    }

    SourceCore(ISourceBlock<TOutput> owningSource,
               DataflowBlockOptions dataflowBlockOptions,
               Consumer<ISourceBlock<TOutput>> completeAction,
               BiConsumer<ISourceBlock<TOutput>, Integer> itemsRemovedAction) {
        this(owningSource, dataflowBlockOptions, completeAction, itemsRemovedAction, null);
    }

    SourceCore(ISourceBlock<TOutput> owningSource,
               DataflowBlockOptions dataflowBlockOptions,
               Consumer<ISourceBlock<TOutput>> completeAction,
               BiConsumer<ISourceBlock<TOutput>, Integer> itemsRemovedAction,
               FunctionThreeParameters<ISourceBlock<TOutput>, TOutput, List<TOutput>, Integer> itemCountingFunction) {
        this.owningSource = owningSource;
        this.dataflowBlockOptions = dataflowBlockOptions;
        this.itemsRemovedAction = itemsRemovedAction;
        this.itemCountFunction = itemCountingFunction;
        this.completeAction = completeAction;

        this.targetRegistry = new TargetRegistry<TOutput>(owningSource);
    }

    AutoCloseable LinkTo(ITargetBlock<TOutput> target, DataflowLinkOptions linkOptions) {
        if (target == null)
            throw new IllegalArgumentException("target cannot be null");

        if (linkOptions == null)
            throw new IllegalArgumentException("linkOptions cannot be null.");

        // TODO: do the async stuff with Task/CompletableFuture

        synchronized (getOutgoingLock()) {
            if (!completionReserved) {
                targetRegistry.add(target, linkOptions);
                offerToTargets(target);
                return Common.CreateUnlinker(getOutgoingLock(), targetRegistry, target);
            }
        }

        // TODO: implement the propagatecompletion stuff here

        return AutoCloseables.Nop;
    }

    DataflowBlockOptions getDataflowBlockOptions() {
        return dataflowBlockOptions;
    }

    private boolean offerToTargets() {
        return offerToTargets(null);
    }

    private boolean offerToTargets(ITargetBlock<TOutput> linkToTarget) {
        if (nextMessageReservedFor != null)
            return false;

        DataflowMessageHeader header = new DataflowMessageHeader(DataflowMessageHeader.DEFAULT_VALUE);
        TOutput message = null;
        boolean offerJustToLinkToTarget = false;

        // TODO: do the volatile stuff here.
        if (enableOffering) {
            if (linkToTarget == null)
                return false;
            else
                offerJustToLinkToTarget = true;
        }

        // TODO: do the peak stuff in messages.

        boolean messageWasAccepted = false;
        if (header.isValid()) {
            if (offerJustToLinkToTarget) {
                TryResult<Boolean> result = offerMessageToTarget(header, message, linkToTarget);
            } else {
                TargetRegistry<TOutput>.LinkedTargetInfo cur = targetRegistry.getFirstTargetNode();
                while (cur != null) {
                    TargetRegistry<TOutput>.LinkedTargetInfo next = cur.Next;
                    TryResult<Boolean> result = offerMessageToTarget(header, message, cur.Target);
                    if (result.isSuccess())
                        break;

                    cur = next;
                }

                if (!messageWasAccepted) {
                    synchronized (getValueLock()) {
                        enableOffering = false;
                    }
                }
            }
        }

        if (messageWasAccepted) {
            synchronized (getValueLock()) {
                TOutput dropped;
            }
        }

        return messageWasAccepted;
    }

    private TryResult<Boolean> offerMessageToTarget(DataflowMessageHeader header, TOutput message, ITargetBlock<TOutput> target) {
        DataflowMessageStatus offerResult = target.offerMessage(header, message, owningSource, false);

        if (offerResult == DataflowMessageStatus.Accepted) {
            targetRegistry.Remove(target, true);
            return new TryResult<>(true, true);
        } else if (offerResult == DataflowMessageStatus.DecliningPermanently) {
            targetRegistry.Remove(target);
        } else if (nextMessageReservedFor != null) {
            // message should not be offered to anyone else.
            return new TryResult<>(true, false);
        }

        // allow the message to be offered to someone else.
        return new TryResult<>(false, false);
    }

    /**
     * Adds a message to the source block for propagation.
     * This method must only be used by one thread at a time, and must not be used concurrently with any other producer
     * side methods, eg. AddMessages, Complete.
     *
     * @param item The item to be wrapped in a message to be added.
     */
    void addMessage(TOutput item) {
        if (decliningPermanently) {
            return;
        }

        messages.add(item);

        // TODO: (si) I don't understand the following c# statement or what the equivalent would be in java:
        // Interlocked.MemoryBarrier();

        if (taskForOutputProcessing == null) {
            offerAsyncIfNecessaryWithValueLock();
        }
    }

    private void offerAsyncIfNecessaryWithValueLock() {
        synchronized (getValueLock()) {
            offerAsyncIfNecessary(false, false);
        }
    }

    private void offerAsyncIfNecessary(boolean isReplacementReplica, boolean outgoingLockKnownAcquired) {
        Common.contractAssertMonitorStatus(getValueLock(), true);

        if (taskForOutputProcessing == null && enableOffering && !messages.isEmpty()) {
            offerAsyncIfNecessarySlow(isReplacementReplica, outgoingLockKnownAcquired);
        }
    }

    private void offerAsyncIfNecessarySlow(boolean isReplacementReplica, boolean outgoingLockKnownAcquired) {
        Common.contractAssertMonitorStatus(getValueLock(), true);
        assert taskForOutputProcessing == null && enableOffering && !messages.isEmpty() :
                "The block must be enabled for offering, not currently be processing, and have messages available to process.";

        boolean targetsAvailable = true;
        // TODO: (si) I don't understand this logic or how to replicate it
        //if(outgoingLockKnownAcquired || SSLFlowDelegate.Monitor.holdsLock())

        if (targetsAvailable && !getCanceledOrFaulted()) {
            // TODO: (si) what is the equivalent of creating a new task?
            taskForOutputProcessing = executorService.submit(this::offerMessagesLoopCore);

            // TODO: (si) skipped feature tracing here

            // TODO: (si) what to do here about exceptions?
        }
    }

    /**
     * Task body used to process messages
     **/
    private void offerMessagesLoopCore() {
        // TODO: (si) this assert is not completely 1:1
        assert taskForOutputProcessing != null : "Must be part of the current processing task.";

        try {
            int maxMessagesPerTask = dataflowBlockOptions.getActualMaxMessagesPerTask();

            final int DEFAULT_RELEASE_LOCK_ITERATIONS = 10;
            int releaseLockIterations = getDataflowBlockOptions().getMaxMessagesPerTask() == DataflowBlockOptions.Unbounded ?
                    DEFAULT_RELEASE_LOCK_ITERATIONS : maxMessagesPerTask;

            for (int messageCounter = 0; messageCounter < maxMessagesPerTask && !getCanceledOrFaulted(); ) {
                synchronized (getOutgoingLock()) {
                    for (int lockReleaseCounter = 0; messageCounter < maxMessagesPerTask && lockReleaseCounter < releaseLockIterations && !getCanceledOrFaulted(); ++messageCounter, ++lockReleaseCounter) {
                        if (!offerToTargets()) return;
                    }
                }
            }
        } catch (Exception exc) {
            // TODO: (si) record exceptions
            completeAction.accept(owningSource);
        } finally {
            synchronized (getOutgoingLock()) {
                synchronized (getValueLock()) {
                    // TODO: (si) this is not completely 1:1
                    assert taskForOutputProcessing != null : "Must be part of the current processing task.";
                    taskForOutputProcessing = null;

                    // TODO: (si) missing memory barrier stuff

                    offerAsyncIfNecessary(true, true);
                    completeBlockIfPossible();
                }
            }
        }

    }

    /**
     * Gets whether the source has had cancellation requested or an exception has occurred.
     */
    private boolean getCanceledOrFaulted() {
        // TODO (si): what is a CancellationToken and what is its purpose?
        return false;
    }

    void complete() {
        synchronized(getValueLock()) {
            decliningPermanently = true;

            CompletableFuture.runAsync(() -> {
                synchronized(getOutgoingLock()) {
                    synchronized(getValueLock()) {
                        completeBlockIfPossible();
                    }
                }
            });
        }
    }

    private void completeBlockIfPossible() {
        Common.contractAssertMonitorStatus(getOutgoingLock(), true);
        Common.contractAssertMonitorStatus(getValueLock(), true);

        if (!completionReserved) {
            if (decliningPermanently && // declining permanently, so no more messages will arrive
                    taskForOutputProcessing == null && // no current processing
                    nextMessageReservedFor == null) {  // no pending reservation
                completeBlockIfPossibleSlow();

            }
        }
    }

    private void completeBlockIfPossibleSlow() {
        assert decliningPermanently && taskForOutputProcessing == null && nextMessageReservedFor == null :
                "The block must be declining permanently, there must be no reservations, and there must be no processing tasks";
        Common.contractAssertMonitorStatus(getOutgoingLock(), true);
        Common.contractAssertMonitorStatus(getValueLock(), true);

        if (messages.isEmpty() || getCanceledOrFaulted()) {
            completionReserved = true;

            executorService.submit(this::completeBlockOncePossible);
        }
    }

    /**
     * Complete the block. This must only be called once, and only once all of the completion conditions are met.
     * As such, it must only be called from completeBlockIfPossible.
     */
    private void completeBlockOncePossible() {
        TargetRegistry<TOutput>.LinkedTargetInfo linkedTargets;
        List<Exception> localExceptions;

        synchronized (getOutgoingLock()) {
            linkedTargets = targetRegistry.clearEntryPoints();

            synchronized(getValueLock()) {
                messages.clear();

                // Save a local reference to the exceptions list and null out the field,
                // so that if the target side tries to add an exception this late,
                // it will go to a separate list (that will be ignored.)
                localExceptions = exceptions;
                exceptions = null;
            }
        }

        if(exceptions != null) {
            // TODO (si): need to figure out what to do with exceptions. How do they work with java completionTasks?
        }
        // TODO (si): missing cancellation token stuff
        else {
            completionTask.complete(null);
        }

        targetRegistry.propagateCompletion(linkedTargets);

        // TODO (si): Skipped all tracing stuff here
    }

}
