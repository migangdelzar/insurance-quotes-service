package com.clara.libs.fn;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {

    void accept(T input) throws E;
}
