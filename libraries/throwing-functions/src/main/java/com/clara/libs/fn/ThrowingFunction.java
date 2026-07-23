package com.clara.libs.fn;

@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {

    R apply(T input) throws E;
}
