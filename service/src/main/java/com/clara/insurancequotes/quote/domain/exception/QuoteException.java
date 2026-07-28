package com.clara.insurancequotes.quote.domain.exception;

public abstract class QuoteException extends RuntimeException {

    protected QuoteException(String message) {
        super(message);
    }
}
