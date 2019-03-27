package com.github.randyklex.dataflow;

// TODO: implement IEnumerable
public interface IProducerConsumerQueue<T> {
    boolean add(T item);

    TryResult<T> tryPoll();

    boolean isEmpty();

    int size();

    int sizeSafe(Object syncObj);
}
