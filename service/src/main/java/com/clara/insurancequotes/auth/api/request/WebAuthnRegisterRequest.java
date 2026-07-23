package com.clara.insurancequotes.auth.api.request;

import jakarta.validation.constraints.NotBlank;

public record WebAuthnRegisterRequest(@NotBlank String challengeId, @NotBlank String credentialJson) {}
