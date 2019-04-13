package com.github.randyklex.dataflow;

import java.util.concurrent.Future;
import java.util.function.Consumer;

class Common {

    // An invalid ID to assign for reordering purposes. This value is chosen to be the last
    // of the 64-bit integers that could ever be assigned as a reordering ID.
    static final long INVALID_REORDERING_ID = -1;

    // A well-known message ID for code that will send exactly one
    // message or where the exact message ID is not important.
    static final int SINGLE_MESSAGE_ID = 1;

    static final DataflowMessageHeader SingleMessageHeader = new DataflowMessageHeader(SINGLE_MESSAGE_ID);

    /*
     * Keeping alive processing tasks: maximum number of processed messages.
     */
    static final int KEEP_ALIVE_NUMBER_OF_MESSAGES_THRESHOLD = 1;

    static final int KEEP_ALIVE_BAN_COUNT = 1000;

    static <TOutput> AutoCloseable CreateUnlinker(Object outgoingLock, TargetRegistry<TOutput> targetRegistry, ITargetBlock<TOutput> targetBlock)
    {
        assert outgoingLock != null;
        assert targetRegistry != null;
        assert targetBlock != null;

        return new CachedUnlinkerShim<>(outgoingLock, targetRegistry, targetBlock);
    }

    static class CachedUnlinkerShim<T> implements AutoCloseable
    {
        final Object syncObj;
        TargetRegistry<T> registry;
        ITargetBlock<T> target;

        CachedUnlinkerShim(Object syncObj, TargetRegistry<T> registry, ITargetBlock<T> target)
        {
            this.syncObj = syncObj;
            this.registry = registry;
            this.target = target;
        }

        @Override
        public void close()
        {
            synchronized (syncObj)
            {
                registry.Remove(target);
            }
        }
    }

    static void contractAssertMonitorStatus(Object syncObj, boolean held) {
        assert syncObj != null : "The locked object to check must be provided.";
        assert Thread.holdsLock(syncObj) == held : "The locking schema was not correctly followed.";
    }

    /**
     * Propagate completion of a sourceCompletionTask to target synchronously.
     * @param sourceCompletionTask The task whose completion is to be propagated. It must be completed.
     * @param target The block where completion is propagated.
     * @param exceptionHandler Handler for exceptions from the target. May be null which would propagate the exception to the caller.
     */
    static void propagateCompletion(Future<?> sourceCompletionTask, IDataflowBlock target, Consumer<Exception> exceptionHandler) {
        assert sourceCompletionTask != null : "sourceCompletionTask may not be null";
        assert target != null : "The target where completion is to be propagated may not be null";
        assert sourceCompletionTask.isDone() : "sourceCompletionTask must be completed in order to propagate its completion";

        // TODO (si) : figure out what to do with exceptions

        try {
            // TODO (si): what if there was an exception in the sourceCompletionTask? It must be sent to the target. Must figure out how to retrieve exceptions from Futures.
            target.complete();
        } catch(Exception exc) {
            if(exceptionHandler != null) {
                exceptionHandler.accept(exc);
            } else {
                throw exc;
            }
        }

    }
}
