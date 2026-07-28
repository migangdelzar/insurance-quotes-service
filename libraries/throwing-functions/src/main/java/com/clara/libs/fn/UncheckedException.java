package com.clara.libs.fn;

/** Wraps a checked exception crossing a functional-interface boundary. */
public class UncheckedException extends RuntimeException {

    public UncheckedException(Exception cause) {
        super(cause.getMessage(), cause);
    }
}
