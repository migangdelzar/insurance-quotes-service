package com.clara.insurancequotes.auth.application.service;

import com.clara.insurancequotes.auth.api.usecase.LogoutUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Coordinates refresh-token revocation for logout. */
@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final RefreshTokenService refreshTokens;

    @Override
    public void logout(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }
}
