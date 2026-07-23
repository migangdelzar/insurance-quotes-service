package com.clara.insurancequotes.auth.api.model;

public record TokenResponse(String accessToken, long expiresInSeconds) {}
