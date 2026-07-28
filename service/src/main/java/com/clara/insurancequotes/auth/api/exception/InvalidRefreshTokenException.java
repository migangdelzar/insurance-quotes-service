package com.clara.insurancequotes.auth.api.exception;

public class InvalidRefreshTokenException extends AuthException {

    public InvalidRefreshTokenException() {
        super("Refresh token is invalid or expired");
    }
}
