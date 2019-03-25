package com.squarespace.dataflow;

class SingleProducerSingleConsumerQueue<T> implements IProducerConsumerQueue<T> {

    private static final int INIT_SEGMENT_SIZE = 32; // must be a power of 2

    private static final int MAX_SEGMENT_SIUZE = 0x1000000; // this could be made as a large as int32.maxValue / 2

    private

    private class Segment
    {
        Segment next;
        final T[] array;

        SegmentState state;
    }

    private class SegmentState
    {
        PaddingFor32 pad0;

        int first;

        int lastCopy;

        PaddingFor32 pad1;

        int firstCopy;

        int last;

        PaddingFor32 pad2;
    }
}


