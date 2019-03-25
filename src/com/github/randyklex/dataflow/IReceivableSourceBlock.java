package com.github.randyklex.dataflow;

import java.util.List;
import java.util.function.Predicate;

public interface IReceivableSourceBlock<TOutput> extends ISourceBlock<TOutput> {

    TryResult<TOutput> TryReceive(Predicate<TOutput> filter);

    TryResult<List<TOutput>> TryReceiveAll();
}
