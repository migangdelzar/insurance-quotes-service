package com.clara.insurancequotes.auth.api.exception;

public class PasskeyNotRegisteredException extends AuthException {

    public PasskeyNotRegisteredException() {
        super("No passkey is registered for this account. Sign in with your password to set one up.");
    }
}
