package com.clara.insurancequotes.auth.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

/** HTTP credentials submitted to the login endpoint. */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
