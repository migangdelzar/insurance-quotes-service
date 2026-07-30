package com.clara.insurancequotes.auth.api.result;

public record WebAuthnChallenge(String challengeId, String publicKeyOptionsJson) {}
