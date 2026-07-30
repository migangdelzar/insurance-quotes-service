package com.clara.insurancequotes.auth.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

/** Browser WebAuthn registration payload. */
public record WebAuthnRegisterRequest(@NotBlank String challengeId, @NotBlank String credentialJson) {}
