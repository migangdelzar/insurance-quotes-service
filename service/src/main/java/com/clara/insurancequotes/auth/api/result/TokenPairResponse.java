package com.clara.insurancequotes.auth.api.result;

public record TokenPairResponse(String accessToken, String refreshToken, long expiresInSeconds) {}
