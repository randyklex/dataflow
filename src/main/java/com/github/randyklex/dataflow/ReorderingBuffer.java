package com.github.randyklex.dataflow;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.function.BiConsumer;

public class ReorderingBuffer<TOutput> implements IReorderingBuffer {
    private final Object owningSource;
    private final HashMap<Long, AbstractMap.SimpleEntry<Boolean, TOutput>> reorderingBuffer = new HashMap<>();
    private final BiConsumer<Object, TOutput> outputAction;
    private long nextReorderedIdToOutput = 0;

    private Object getValueLock() { return reorderingBuffer; }

    ReorderingBuffer(Object owningSource, BiConsumer<Object, TOutput> outputAction)
    {
        this.owningSource = owningSource;
        this.outputAction = outputAction;
    }

    void addItem(long id, TOutput item, boolean itemIsValid)
    {
        // TODO: implement assertions;

        synchronized (getValueLock())
        {
            if (nextReorderedIdToOutput == id)
            {
                outputNextItem(item, itemIsValid);
            }
            else
            {
                reorderingBuffer.put(id, new AbstractMap.SimpleEntry<>(itemIsValid, item));
            }
        }
    }

    public void ignoreItem(long id)
    {
        addItem(id, null, false);
    }

    private void outputNextItem(TOutput theNextItem, boolean itemIsValid)
    {
        nextReorderedIdToOutput++;

        if (itemIsValid)
            outputAction.accept(owningSource, theNextItem);

        AbstractMap.SimpleEntry<Boolean, TOutput> nextOutputItemWithValidity;

        nextOutputItemWithValidity = reorderingBuffer.getOrDefault(nextReorderedIdToOutput, null);
        while (nextOutputItemWithValidity != null)
        {
            reorderingBuffer.remove(nextReorderedIdToOutput);
            nextReorderedIdToOutput++;

            if (nextOutputItemWithValidity.getKey())
                outputAction.accept(owningSource, nextOutputItemWithValidity.getValue());

            nextOutputItemWithValidity = reorderingBuffer.getOrDefault(nextReorderedIdToOutput, null);
        }
    }

    // TODO: implement the debug stuff.
}
