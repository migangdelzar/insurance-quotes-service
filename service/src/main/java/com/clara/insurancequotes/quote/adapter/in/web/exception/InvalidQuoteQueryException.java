package com.clara.insurancequotes.quote.adapter.in.web.exception;

/** Indicates that HTTP query parameters could not be translated to a Quote query. */
public class InvalidQuoteQueryException extends RuntimeException {

    public InvalidQuoteQueryException(String message) {
        super(message);
    }
}
