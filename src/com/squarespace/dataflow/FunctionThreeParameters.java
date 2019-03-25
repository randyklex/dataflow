package com.squarespace.dataflow;

@FunctionalInterface
public interface FunctionThreeParameters<T, U, V, R> {
    public R apply(T t, U u, V v);
}
