package com.clara.insurancequotes.auth.application.exception;

public class InvalidCredentialsException extends AuthException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
