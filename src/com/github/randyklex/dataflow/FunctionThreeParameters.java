package com.github.randyklex.dataflow;

@FunctionalInterface
public interface FunctionThreeParameters<T, U, V, R> {
    R apply(T t, U u, V v);
}
