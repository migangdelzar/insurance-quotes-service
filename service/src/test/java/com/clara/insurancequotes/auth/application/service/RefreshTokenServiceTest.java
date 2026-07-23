package com.clara.insurancequotes.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.clara.insurancequotes.auth.api.exception.InvalidRefreshTokenException;
import com.clara.insurancequotes.auth.domain.model.User;
import com.clara.insurancequotes.testsupport.InMemoryRefreshTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-22T10:00:00Z");

    private final InMemoryRefreshTokenRepository repository = new InMemoryRefreshTokenRepository();
    private final RefreshTokenService service =
            new RefreshTokenService(repository, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofDays(7));
    private final User user = User.create("demo", "$2a$10$hash", NOW);

    @Test
    void issueAndRotate_returnsNewTokenAndRevokesOld() {
        var raw = service.issue(user);

        var rotated = service.rotate(raw);

        assertThat(rotated.rawToken()).isNotEqualTo(raw);
        assertThatThrownBy(() -> service.rotate(raw)).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void replayOfRotatedToken_revokesWholeFamily() {
        var raw = service.issue(user);
        var rotated = service.rotate(raw);

        assertThatThrownBy(() -> service.rotate(raw)).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.rotate(rotated.rawToken())).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void expiredToken_isRejected() {
        var shortLived = new RefreshTokenService(repository, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ZERO);
        var raw = shortLived.issue(user);

        assertThatThrownBy(() -> service.rotate(raw)).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void unknownToken_isRejected() {
        assertThatThrownBy(() -> service.rotate("garbage")).isInstanceOf(InvalidRefreshTokenException.class);
    }
}
