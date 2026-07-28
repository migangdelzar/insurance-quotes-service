package com.clara.libs.fn;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Bridges throwing functional interfaces to java.util.function equivalents. */
public final class Unchecked {

    private Unchecked() {}

    public static <T, R> Function<T, R> function(ThrowingFunction<T, R, ?> fn) {
        return input -> {
            try {
                return fn.apply(input);
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new UncheckedException(exception);
            }
        };
    }

    public static <T> Supplier<T> supplier(ThrowingSupplier<T, ?> fn) {
        return () -> {
            try {
                return fn.get();
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new UncheckedException(exception);
            }
        };
    }

    public static <T> Consumer<T> consumer(ThrowingConsumer<T, ?> fn) {
        return input -> {
            try {
                fn.accept(input);
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new UncheckedException(exception);
            }
        };
    }

    public static Runnable runnable(ThrowingRunnable<?> fn) {
        return () -> {
            try {
                fn.run();
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new UncheckedException(exception);
            }
        };
    }

    public static <T, U, R> BiFunction<T, U, R> biFunction(ThrowingBiFunction<T, U, R, ?> fn) {
        return (first, second) -> {
            try {
                return fn.apply(first, second);
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new UncheckedException(exception);
            }
        };
    }
}
