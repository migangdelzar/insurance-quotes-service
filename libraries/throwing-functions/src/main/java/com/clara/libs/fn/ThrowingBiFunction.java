package com.clara.libs.fn;

@FunctionalInterface
public interface ThrowingBiFunction<T, U, R, E extends Exception> {

    R apply(T first, U second) throws E;
}
