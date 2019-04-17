package com.github.randyklex.dataflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class BufferBlockTest {

    @Test
    public void testCtor() {

    }

    @Test
    public void testArgumentExceptions() {
        assertThrows(NullPointerException.class, () -> new BufferBlock<Integer>(null));
        DataflowTestHelpers.testArgumentsExceptions(new BufferBlock<Integer>());
    }
}
