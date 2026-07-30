package com.clara.insurancequotes.auth.api.usecase;

import com.clara.insurancequotes.auth.api.result.WebAuthnChallenge;

/** Starts a passkey registration ceremony for an authenticated user. */
public interface StartPasskeyRegistrationUseCase {

    WebAuthnChallenge startRegistration(String username);
}
