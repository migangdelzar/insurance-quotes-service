package com.clara.insurancequotes.quote.api.exception;

public abstract class QuoteApplicationException extends RuntimeException {

    protected QuoteApplicationException(String message) {
        super(message);
    }
}
