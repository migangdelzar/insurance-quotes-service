package com.clara.insurancequotes.quote.application.exception;

public abstract class QuoteApplicationException extends RuntimeException {

    protected QuoteApplicationException(String message) {
        super(message);
    }
}
