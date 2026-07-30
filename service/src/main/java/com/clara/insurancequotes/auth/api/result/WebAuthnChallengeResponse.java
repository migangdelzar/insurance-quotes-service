package com.clara.insurancequotes.auth.api.result;

public record WebAuthnChallengeResponse(String challengeId, String publicKeyOptionsJson) {}
