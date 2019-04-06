package com.github.randyklex.dataflow;

/**
 * Represents a dataflow block that is both a target for data and a source of data.
 * @param <TInput> Specifies the type of data accepted by the IPropagatorBlock.
 * @param <TOutput> Specifies the type of data supplied by the IPropagatorBlock.
 */
public interface IPropagatorBlock<TInput, TOutput> extends ITargetBlock<TInput>, ISourceBlock<TOutput> {
    // No additional members beyond those inherited from ITargetBlock<TInput> and ISourceBlock<TOutput>
}
