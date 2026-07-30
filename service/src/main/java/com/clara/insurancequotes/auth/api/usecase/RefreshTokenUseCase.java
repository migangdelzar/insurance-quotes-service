package com.clara.insurancequotes.auth.api.usecase;

import com.clara.insurancequotes.auth.api.result.TokenPair;

/** Rotates a refresh token and issues a new access-token pair. */
public interface RefreshTokenUseCase {

    TokenPair refresh(String refreshToken);
}
