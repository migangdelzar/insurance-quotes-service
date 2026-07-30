package com.clara.insurancequotes.auth.api.usecase;

import com.clara.insurancequotes.auth.api.result.WebAuthnChallenge;

/** Starts a passkey assertion ceremony for an optional username. */
public interface StartPasskeyAssertionUseCase {

    WebAuthnChallenge startAssertion(String username);
}
