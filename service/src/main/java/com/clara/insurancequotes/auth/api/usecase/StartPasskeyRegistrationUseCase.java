package com.clara.insurancequotes.auth.api.usecase;

import com.clara.insurancequotes.auth.api.result.WebAuthnChallengeResponse;

/** Starts a passkey registration ceremony for an authenticated user. */
public interface StartPasskeyRegistrationUseCase {

    WebAuthnChallengeResponse startRegistration(String username);
}
