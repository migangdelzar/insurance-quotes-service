package com.clara.insurancequotes.auth.api.result;

public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {}
