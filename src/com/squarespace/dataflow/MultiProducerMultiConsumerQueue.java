package com.squarespace.dataflow;

import java.util.concurrent.ConcurrentLinkedQueue;

public class MultiProducerMultiConsumerQueue<T> extends ConcurrentLinkedQueue<T> implements IProducerConsumerQueue<T>  {

    @Override
    public boolean add(T item)
    {
        return super.add(item);
    }

    @Override
    public boolean offer(T item)
    {
        return super.offer(item);
    }

    @Override
    public boolean isEmpty()
    {
        return super.isEmpty();
    }

    @Override
    public int size()
    {
        return super.size();
    }

    @Override
    public TryResult<T> tryPoll()
    {
        T rval = super.poll();

        if (rval != null)
            return new TryResult(true, rval);
        else
            return new TryResult<>(false, null);
    }
}
