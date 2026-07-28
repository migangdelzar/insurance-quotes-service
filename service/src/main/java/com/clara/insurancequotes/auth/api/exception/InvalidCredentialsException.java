package com.clara.insurancequotes.auth.api.exception;

public class InvalidCredentialsException extends AuthException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
