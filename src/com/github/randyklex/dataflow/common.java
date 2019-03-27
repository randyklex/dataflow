package com.github.randyklex.dataflow;

class common {

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


}
