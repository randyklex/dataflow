package com.github.randyklex.dataflow;

import java.util.function.Predicate;

public abstract class SourceDataflowBlock {

    public SourceDataflowBlock()
    { }

    public <TOutput> AutoCloseable LinkTo(ITargetBlock<TOutput> targetBlock)
    {
        if (targetBlock == null)
            throw new IllegalArgumentException("targetBlock cannot be null.");

        return LinkTo(targetBlock, DataflowLinkOptions.Default, null);
    }

    public <TOutput> AutoCloseable LinkTo(ITargetBlock<TOutput> targetBlock, Predicate<TOutput> predicate)
    {
        return LinkTo(targetBlock, DataflowLinkOptions.Default, predicate);
    }

    public <TOutput> AutoCloseable LinkTo(ITargetBlock<TOutput> targetBlock, DataflowLinkOptions linkOptions, Predicate<TOutput> predicate)
    {
        if (targetBlock == null)
            throw new IllegalArgumentException("targetBlock cannot be null.");

        if (linkOptions == null)
            throw new IllegalArgumentException("linkOptions cannot be null.");

        // TODO: the original source doesn't allow for NULL here, but the first linkTo will call this with a NULL predicate.. hhhmmm??
        if (predicate == null)
            throw new IllegalArgumentException("predicate cannot be null.");
    }
}
