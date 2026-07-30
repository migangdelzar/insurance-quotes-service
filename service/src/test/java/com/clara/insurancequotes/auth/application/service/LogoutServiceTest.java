package com.clara.insurancequotes.auth.application.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class LogoutServiceTest {

    private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    private final LogoutService service = new LogoutService(refreshTokens);

    @Test
    void logout_revokesRefreshToken() {
        service.logout("refresh-token");

        verify(refreshTokens).revoke("refresh-token");
    }
}
