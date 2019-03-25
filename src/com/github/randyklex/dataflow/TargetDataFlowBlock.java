package com.github.randyklex.dataflow;

import java.util.concurrent.CompletableFuture;

public class TargetDataFlowBlock<TInput> implements ITargetBlock<TInput> {
    public TargetDataFlowBlock()
    { }

    public boolean Post(TInput item)
    {
        return this.offerMessage(common.SingleMessageHeader, item, null, false) == DataflowMessageStatus.Accepted;
    }

    public CompletableFuture<boolean> SendAsync(TInput item)
    {
        return SendAsync(item, CancellationToken.None);
    }
}
