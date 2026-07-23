package com.clara.insurancequotes.auth.api.exception;

public abstract class AuthException extends RuntimeException {

    protected AuthException(String message) {
        super(message);
    }
}
