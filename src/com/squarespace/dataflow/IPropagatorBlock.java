package com.squarespace.dataflow;

public interface IPropagatorBlock<TInput, TOutput> extends ITargetBlock<TInput>, ISourceBlock<TOutput> {

}
