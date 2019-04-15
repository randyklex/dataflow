package com.github.randyklex.dataflow;


import org.junit.jupiter.api.Test;

import javax.xml.crypto.dsig.Transform;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


public class TransformBlockTest {

    final int TEST_OFFERING = 4242;

    @Test
    public void testCtor() {

        ExecutionDataflowBlockOptions opts = new ExecutionDataflowBlockOptions();
        opts.setMaxMessagesPerTask(1);

        List<TransformBlock<Integer, String>> blocks = Arrays.asList(
                new TransformBlock<>(Object::toString),
                new TransformBlock<>(Object::toString, opts)

                // TODO (si): asynchronous case
                //new TransformBlock<>(Object::toString, opts),
        );

        for (TransformBlock<Integer, String> block : blocks) {
            assertEquals(0, block.getInputSize());
            assertEquals(0, block.getOutputSize());
            assertFalse(block.getCompletion().isDone());
        }
    }

    @Test
    public void testArgumentException() {
        // TODO (si)
    }

    @Test
    public void testtoString() {
        // TODO (si)
    }

    @Test
    public void testOfferMessage() {
        ExecutionDataflowBlockOptions opts = new ExecutionDataflowBlockOptions();
        opts.setMaxMessagesPerTask(10);

        List<Supplier<TransformBlock<Integer, Integer>>> generators = Arrays.asList(
                () -> new TransformBlock<>(i -> i),
                () -> new TransformBlock<>(i -> i, opts)
        );

        for (Supplier<TransformBlock<Integer, Integer>> generator : generators) {
            DataflowTestHelpers.testOfferMessage_argumentValidation(generator.get(), TEST_OFFERING);

            TransformBlock<Integer, Integer> target = generator.get();
            DataflowTestHelpers.testOfferMessage_acceptsDataDirectly(target, TEST_OFFERING);
            DataflowTestHelpers.testOfferMessage_completeAndOffer(target, TEST_OFFERING);

            target = generator.get();
            DataflowTestHelpers.testOfferMessage_acceptsViaLinking(target, TEST_OFFERING);
            DataflowTestHelpers.testOfferMessage_completeAndOffer(target, TEST_OFFERING);
        }
    }
}
