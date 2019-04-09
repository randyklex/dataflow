package com.github.randyklex.dataflow;

public final class AutoCloseables {
    static final AutoCloseable Nop = new NopAutoCloseable();

    static class NopAutoCloseable implements AutoCloseable
    {
        public void close() {}
    }
}
