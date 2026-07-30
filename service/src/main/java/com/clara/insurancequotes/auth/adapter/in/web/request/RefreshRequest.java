package com.clara.insurancequotes.auth.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

/** HTTP refresh-token payload used by refresh and logout endpoints. */
public record RefreshRequest(@NotBlank String refreshToken) {}
