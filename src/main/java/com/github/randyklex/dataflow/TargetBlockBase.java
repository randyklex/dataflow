package com.github.randyklex.dataflow;

import java.util.function.Predicate;

public abstract class TargetBlockBase<TInput> implements ITargetBlock<TInput> {
    public AutoCloseable linkTo(ITargetBlock<TInput> targetBlock)
    {
        return this.linkTo(targetBlock, DataflowLinkOptions.Default);
    }

    public AutoCloseable linkTo(ITargetBlock<TInput> targetBlock, Predicate<TInput> predicate)
    {
        return this.linkTo(targetBlock, DataflowLinkOptions.Default, predicate);
    }

    public abstract AutoCloseable linkTo(ITargetBlock<TInput> targetBlock, DataflowLinkOptions linkOptions);

    public abstract AutoCloseable linkTo(ITargetBlock<TInput> targetBlock, DataflowLinkOptions linkOptions, Predicate<TInput> predicate);
}
