package com.github.randyklex.dataflow;

import java.util.concurrent.CompletableFuture;

public interface IDataflowBlock {

    CompletableFuture<?> getCompletion();

    void complete();

    void fault(Exception exception);
}
