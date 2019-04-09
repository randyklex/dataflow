package com.github.randyklex.dataflow;

public interface ITargetBlock<TInput> extends IDataflowBlock {

    DataflowMessageStatus offerMessage(DataflowMessageHeader messageHeader,
                                       TInput messageValue,
                                       ISourceBlock<TInput> source,
                                       boolean consumeToAccept);
}
