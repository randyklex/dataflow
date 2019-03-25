package com.squarespace.dataflow;

public interface IProducerConsumerQueue<T> {
    boolean add(T item);

    boolean offer(T item);

    TryResult<T> tryPoll();

    boolean isEmpty();

    int size();
}
