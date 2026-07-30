package com.clara.insurancequotes.auth.api.usecase;

import com.clara.insurancequotes.auth.api.result.TokenPairResponse;

/** Rotates a refresh token and issues a new access-token pair. */
public interface RefreshTokenUseCase {

    TokenPairResponse refresh(String refreshToken);
}
