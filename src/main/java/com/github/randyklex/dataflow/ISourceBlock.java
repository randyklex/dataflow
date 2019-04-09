package com.github.randyklex.dataflow;

import java.util.function.Predicate;

public interface ISourceBlock<TOutput> extends IDataflowBlock {

    AutoCloseable linkTo(ITargetBlock<TOutput> target);

    AutoCloseable linkTo(ITargetBlock<TOutput> target, DataflowLinkOptions linkOptions);

    AutoCloseable linkTo(ITargetBlock<TOutput> target, Predicate<TOutput> predicate);

    AutoCloseable linkTo(ITargetBlock<TOutput> target, DataflowLinkOptions linkOptions, Predicate<TOutput> predicate);

    TryResult<TOutput> consumeMessage(DataflowMessageHeader messageHeader, ITargetBlock<TOutput> target);

    boolean reserveMessage(DataflowMessageHeader messageHeader, ITargetBlock<TOutput> target);

    void releaseReservation(DataflowMessageHeader messageHeader, ITargetBlock<TOutput> target);

}
