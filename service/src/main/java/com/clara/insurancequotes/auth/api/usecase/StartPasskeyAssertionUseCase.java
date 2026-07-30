package com.clara.insurancequotes.auth.api.usecase;

import com.clara.insurancequotes.auth.api.result.WebAuthnChallengeResponse;

/** Starts a passkey assertion ceremony for an optional username. */
public interface StartPasskeyAssertionUseCase {

    WebAuthnChallengeResponse startAssertion(String username);
}
