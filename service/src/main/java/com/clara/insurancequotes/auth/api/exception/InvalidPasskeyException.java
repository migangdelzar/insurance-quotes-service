package com.clara.insurancequotes.auth.api.exception;

public class InvalidPasskeyException extends AuthException {

    public InvalidPasskeyException(String detail) {
        super("Passkey ceremony failed", detail == null ? null : new IllegalArgumentException(detail));
    }
}
