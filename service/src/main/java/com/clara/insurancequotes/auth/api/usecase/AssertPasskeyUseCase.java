package com.clara.insurancequotes.auth.api.usecase;

import com.clara.insurancequotes.auth.api.result.TokenPair;

/** Verifies a passkey assertion and issues an authenticated token pair. */
public interface AssertPasskeyUseCase {

    TokenPair assertPasskey(String challengeId, String credentialJson, String mfaToken);
}
