package com.clara.insurancequotes.auth.api.result;

public record TokenResponse(String accessToken, long expiresInSeconds) {}
