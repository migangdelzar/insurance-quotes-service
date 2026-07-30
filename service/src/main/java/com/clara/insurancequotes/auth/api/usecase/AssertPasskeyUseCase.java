package com.clara.insurancequotes.auth.api.usecase;

import com.clara.insurancequotes.auth.api.result.TokenPairResponse;

/** Verifies a passkey assertion and issues an authenticated token pair. */
public interface AssertPasskeyUseCase {

    TokenPairResponse assertPasskey(String challengeId, String credentialJson, String mfaToken);
}
