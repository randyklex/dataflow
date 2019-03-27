
package com.github.randyklex.dataflow;


import java.util.function.Predicate;

public abstract class DataflowBlock implements IDataflowBlock {


    public <TOutput> AutoCloseable LinkTo(ITargetBlock<TOutput> target)
    {
        if (target == null) throw new IllegalArgumentException("target cannot be null");

        return this.LinkTo(target, DataflowLinkOptions.Default);
    }

    public <TOutput> AutoCloseable LinkTo(ITargetBlock<TOutput> target, Predicate<TOutput> predicate)
    {

    }

    public <TOutput> AutoCloseable LinkTo(ITargetBlock<TOutput> target, DataflowLinkOptions linkOptions, Predicate<TOutput> predicate)
    {
        if (target == null)
            throw new IllegalArgumentException("target cannot be null.");

        if (linkOptions == null)
            throw new IllegalArgumentException("linkOptions cannot be null.");

        if (predicate == null)
            throw new IllegalArgumentException("predicate cannot be null.");

        FilteredLinkPropagator<TOutput> filter = new FilteredLinkPropagator(this, target, predicate);
        return LinkTo(filter, linkOptions);
    }
}
