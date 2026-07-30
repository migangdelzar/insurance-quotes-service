package com.clara.insurancequotes.auth.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

/** Browser WebAuthn assertion payload. */
public record WebAuthnAssertRequest(@NotBlank String challengeId, @NotBlank String credentialJson, String mfaToken) {}
