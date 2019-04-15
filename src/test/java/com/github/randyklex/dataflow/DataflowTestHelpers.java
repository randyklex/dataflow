package com.github.randyklex.dataflow;

//import static org.junit.jupiter.api.Assertions.assertThro


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DataflowTestHelpers {
    static boolean[] booleanValues = {true, false};

    static <T> void testOfferMessage_argumentValidation(ITargetBlock<T> target, T offering) {
        // TODO (si) : The first test relies on the existence of the BufferBlock
        //target.offerMessage(() -> { target.offerMessage(new DataflowMessageHeader(0), offering, new BufferBlock) })

        assertThrows(IllegalArgumentException.class, () ->
                target.offerMessage(new DataflowMessageHeader(0), offering, null, false));
        assertThrows(IllegalArgumentException.class, () ->
                target.offerMessage(new DataflowMessageHeader(1), offering, null, true));
    }

    static <T> void testOfferMessage_acceptsDataDirectly(ITargetBlock<T> target, T offering) {
        testOfferMessage_acceptsDataDirectly(target, offering, 3);

    }

    static <T> void testOfferMessage_acceptsDataDirectly(ITargetBlock<T> target, T offering, int messages) {
        for (int i = 1; i <= messages; ++i) {
            assertEquals(DataflowMessageStatus.Accepted, target.offerMessage(new DataflowMessageHeader(i), offering, null, false));
        }
    }

    static <T> void testOfferMessage_completeAndOffer(ITargetBlock<T> target, T offering) {
        testOfferMessage_completeAndOffer(target, offering, 3);
    }

    static <T> void testOfferMessage_completeAndOffer(ITargetBlock<T> target, T offering, int messages) {
        target.complete();
        for (int i = 1; i <= messages; ++i) {
            assertEquals(DataflowMessageStatus.DecliningPermanently, target.offerMessage(new DataflowMessageHeader(4), offering, null, false));
        }
    }

    static <T> void testOfferMessage_acceptsViaLinking(ITargetBlock<T> target, T offering) {
        testOfferMessage_acceptsViaLinking(target, offering, 3);
    }

    static <T> void testOfferMessage_acceptsViaLinking(ITargetBlock<T> target, T offering, int messages) {
        // TODO (si) : Needs the buffer block
    }
}
