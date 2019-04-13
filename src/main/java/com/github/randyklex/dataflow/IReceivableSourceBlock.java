package com.github.randyklex.dataflow;

import java.util.List;
import java.util.function.Predicate;

/**
 * Represents a dataflow block that supports receiving of messages without linking.
 * @param <TOutput> Specifies the type of data supplied by the IReceivableSourceBlock.
 */
public interface IReceivableSourceBlock<TOutput> extends ISourceBlock<TOutput> {

    TryResult<TOutput> TryReceive(Predicate<TOutput> filter);

    TryResult<List<TOutput>> TryReceiveAll();
}
