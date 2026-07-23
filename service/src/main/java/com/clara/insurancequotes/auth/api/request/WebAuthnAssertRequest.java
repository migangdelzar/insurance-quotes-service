package com.clara.insurancequotes.auth.api.request;

import jakarta.validation.constraints.NotBlank;

public record WebAuthnAssertRequest(@NotBlank String challengeId, @NotBlank String credentialJson, String mfaToken) {}
