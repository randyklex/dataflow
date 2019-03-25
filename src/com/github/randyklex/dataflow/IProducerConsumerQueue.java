package com.github.randyklex.dataflow;

public interface IProducerConsumerQueue<T> {
    boolean add(T item);

    boolean offer(T item);

    TryResult<T> tryPoll();

    boolean isEmpty();

    int size();
}
