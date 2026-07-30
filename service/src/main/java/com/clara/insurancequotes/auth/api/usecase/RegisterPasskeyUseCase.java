package com.clara.insurancequotes.auth.api.usecase;

/** Completes a passkey registration ceremony. */
public interface RegisterPasskeyUseCase {

    void register(String challengeId, String credentialJson);
}
