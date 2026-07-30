package com.clara.insurancequotes.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import com.clara.insurancequotes.auth.domain.model.User;
import com.clara.insurancequotes.auth.domain.model.UserRole;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshAccessTokenServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-07-22T10:00:00Z");

    private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final RefreshAccessTokenService service = new RefreshAccessTokenService(refreshTokens, users, tokenService);

    @Test
    void refresh_rotatesTokenAndIssuesAccessTokenPair() {
        var user = User.create("demo", "hash", UserRole.USER, NOW);
        when(refreshTokens.rotate("old-refresh"))
                .thenReturn(new RefreshTokenService.Rotation(USER_ID, "new-refresh"));
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(tokenService.issueApiToken(user)).thenReturn(new TokenService.IssuedAccess("access", 1800));

        var result = service.refresh("old-refresh");

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
        assertThat(result.expiresInSeconds()).isEqualTo(1800);
        verify(refreshTokens).rotate("old-refresh");
    }
}
