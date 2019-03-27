package com.github.randyklex.dataflow;

import java.lang.reflect.Array;

class SingleProducerSingleConsumerQueue<T> implements IProducerConsumerQueue<T> {

    private static final int INIT_SEGMENT_SIZE = 32; // must be a power of 2
    private static final int MAX_SEGMENT_SIZE = 0x1000000; // this could be made as a large as int32.maxValue / 2
    private Segment<T> head;
    private Segment<T> tail;

    SingleProducerSingleConsumerQueue()
    {
        head = tail = new Segment<>(INIT_SEGMENT_SIZE);
    }

    public boolean add(T item)
    {
        Segment segment = tail;
        T[] array = (T[])segment.array;

        int last = segment.state.last;
        int tail2 = (last + 1) & (array.length - 1);
        if (tail2 != segment.state.firstCopy)
        {
            array[last] = item;
            segment.state.last = tail2;
        }
        else
            addSlow(item, segment);

        return true;
    }

    private void addSlow(T item, Segment segment)
    {
        assert segment != null;

        if (segment.state.firstCopy != segment.state.first)
        {
            segment.state.firstCopy  = segment.state.first;
            add(item);
            return;
        }

        int newSegmentSize = tail.array.length << 1; // double the size
        if (newSegmentSize > MAX_SEGMENT_SIZE) newSegmentSize = MAX_SEGMENT_SIZE;

        Segment newSegment = new Segment(newSegmentSize);
        newSegment.array[0] = item;
        newSegment.state.last = 1;
        newSegment.state.lastCopy =1;

        try
        {}
        finally
        {
            // TODO: implement the volatile stuff here
            tail.next = newSegment;
            tail = newSegment;
        }
    }

    public TryResult<T> tryPoll()
    {
        Segment segment = head;
        T[] array = (T[])segment.array;
        // TODO: verify this line below
        int first = segment.state.first; // local copy to avoid multiple volatile reads.

        if (first != segment.state.lastCopy)
        {
            TryResult<T> result = new TryResult(true, array[first]);
            array[first] = null;
            segment.state.first = (first + 1) & (array.length - 1);
            return result;
        }
        else
            return tryPollSlow(segment, array);
    }

    private TryResult<T> tryPollSlow(Segment segment, T[] array)
    {
        if (segment.state.last != segment.state.lastCopy)
        {
            segment.state.lastCopy = segment.state.last;
            return tryPoll();
        }

        if (segment.next != null && segment.state.first == segment.state.last)
        {
            segment = segment.next;
            array = (T[])segment.array;
            head = segment;
        }

        int first = segment.state.first; // local copy to avoid extraneous volatile reads
        if (first == segment.state.last)
        {
            TryResult<T> result = new TryResult(false, null);
            return result;
        }

        TryResult<T> result = new TryResult<>(true, array[first]);
        array[first] = null;
        segment.state.first = (first + 1) & (segment.array.length - 1);
        segment.state.lastCopy = segment.state.last;

        return result;
    }

    // TODO: implement TryPeek and TryPeek Slow

    // TODO: implement TryPollIf and TryPollIfSlow

    public void clear()
    {
        TryResult<T> result = tryPoll();
        while (result.isSuccess())
        {
            result = tryPoll();
        }
    }

    public boolean isEmpty()
    {
        Segment head = this.head;
        if (head.state.first != head.state.lastCopy) return false;
        if (head.state.first != head.state.last) return false;
        return head.next == null;
    }

    // TODO: implement an Enumerator?? maybe?

    public int size()
    {
        int count = 0;
        for (Segment segment = head; segment != null; segment = segment.next)
        {
            int arraySize = segment.array.length;
            int first, last;
            while (true)
            {
                first = segment.state.first;
                last = segment.state.last;
                if (first == segment.state.first)
                    break;
            }
            count += (last - first) & (arraySize - 1);
        }

        return count;
    }

    public int sizeSafe(Object syncObj)
    {
        synchronized (syncObj)
        {
            return size();
        }
    }

    private class Segment<T1>
    {
        Segment next;
        final T1[] array;

        SegmentState state;

        Segment(int size)
        {
            array = (T1[])(new Object[size]);
        }
    }

    private class SegmentState
    {
        // TODO: implement padding
        int first;

        int lastCopy;

        int firstCopy;

        int last;
    }
}


