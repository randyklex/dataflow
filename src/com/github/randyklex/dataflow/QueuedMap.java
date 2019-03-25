package com.github.randyklex.dataflow;

import java.util.*;

final class QueuedMap<TKey, TValue> {

    private final ArrayBasedLinkedQueue<AbstractMap.SimpleEntry<TKey, TValue>> queue;

    private final HashMap<TKey, Integer> mapKeyToIndex;

    QueuedMap()
    {
        queue = new ArrayBasedLinkedQueue<>();
        mapKeyToIndex = new HashMap<>();
    }

    QueuedMap(int capacity)
    {
        queue = new ArrayBasedLinkedQueue<>(capacity);
        mapKeyToIndex = new HashMap<>(capacity);
    }

    void put(TKey key, TValue value)
    {
        int indexOfKeyInQueue;

        if (mapKeyToIndex.containsKey(key)) {
            indexOfKeyInQueue = mapKeyToIndex.get(key);
            queue.replace(indexOfKeyInQueue, new AbstractMap.SimpleEntry<TKey, TValue>(key, value));
        }
        else
        {
            indexOfKeyInQueue = queue.add(new AbstractMap.SimpleEntry<>(key, value));
            mapKeyToIndex.put(key, indexOfKeyInQueue);
        }
    }

    TryResult<AbstractMap.SimpleEntry<TKey, TValue>> tryPoll()
    {
        TryResult<AbstractMap.SimpleEntry<TKey, TValue>> result = queue.tryPoll();
        if (result.isSuccess())
            mapKeyToIndex.remove(result.getResult().getKey());

        return result;
    }

    // TODO: implement the PopRange

    int size() {
        return mapKeyToIndex.size();
    }

    private final class ArrayBasedLinkedQueue<T>
    {
        private static final int TERMINATOR_INDEX = -1;

        private final List<AbstractMap.SimpleEntry<Integer, T>> storage;

        private int headIndex = TERMINATOR_INDEX;
        private int tailIndex = TERMINATOR_INDEX;
        private int freeIndex = TERMINATOR_INDEX;

        ArrayBasedLinkedQueue()
        {
            storage = new ArrayList<>();
        }

        ArrayBasedLinkedQueue(int capacity)
        {
            storage = new ArrayList<>(capacity);
        }

        int add(T item)
        {
            int newIndex;

            if (freeIndex != TERMINATOR_INDEX)
            {
                newIndex = freeIndex;
                freeIndex = storage.get(freeIndex).getKey();
                storage.add(newIndex, new AbstractMap.SimpleEntry<>(TERMINATOR_INDEX, item));
            }
            else
            {
                newIndex = storage.size();
                storage.add(new AbstractMap.SimpleEntry<>(TERMINATOR_INDEX, item));
            }

            if (headIndex == TERMINATOR_INDEX)
            {
                headIndex = newIndex;
            }
            else
            {
                storage.add(tailIndex, new AbstractMap.SimpleEntry<>(newIndex, storage.get(tailIndex).getValue()));
            }

            tailIndex = newIndex;
            return newIndex;
        }

        TryResult<T> tryPoll()
        {
            T item;

            if (headIndex == TERMINATOR_INDEX)
            {
                TryResult<T> result = new TryResult<>(false, null);
                return result;
            }

            // TODO: insert the assertion here.
            item = storage.get(headIndex).getValue();

            int newHeadIndex = storage.get(headIndex).getKey();
            storage.add(headIndex, new AbstractMap.SimpleEntry<>(freeIndex, null));
            freeIndex = headIndex;
            headIndex = newHeadIndex;
            if (headIndex == TERMINATOR_INDEX) tailIndex = TERMINATOR_INDEX;

            return new TryResult<>(true, item);
        }

        void replace(int index, T item)
        {

            // TODO: implement the assertions here.

            AbstractMap.SimpleEntry<Integer, T> entry = storage.get(index);
            storage.remove(entry);
            storage.add(index, new AbstractMap.SimpleEntry<Integer, T>(entry.getKey(), item));
        }

        boolean isEmpty() { return headIndex == TERMINATOR_INDEX; }
    }


}
