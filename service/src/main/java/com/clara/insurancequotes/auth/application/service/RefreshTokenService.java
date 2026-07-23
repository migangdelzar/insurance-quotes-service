package com.clara.insurancequotes.auth.application.service;

import com.clara.insurancequotes.auth.api.exception.InvalidRefreshTokenException;
import com.clara.insurancequotes.auth.application.port.out.RefreshTokenRepository;
import com.clara.insurancequotes.auth.domain.model.RefreshToken;
import com.clara.insurancequotes.auth.domain.model.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    public record Rotation(UUID userId, String rawToken) {}

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final Clock clock;
    private final Duration ttl;

    public RefreshTokenService(
            RefreshTokenRepository repository, Clock clock, @Value("${auth.refresh.ttl:7d}") Duration ttl) {
        this.repository = repository;
        this.clock = clock;
        this.ttl = ttl;
    }

    @Transactional
    public String issue(User user) {
        return issueInFamily(user.id(), UUID.randomUUID());
    }

    @Transactional
    public Rotation rotate(String rawToken) {
        var token = repository.findByTokenHash(hash(rawToken)).orElseThrow(InvalidRefreshTokenException::new);
        if (token.isRevoked()) {
            repository.revokeFamily(token.familyId(), clock.instant());
            throw new InvalidRefreshTokenException();
        }
        if (token.isExpired(clock.instant())) {
            throw new InvalidRefreshTokenException();
        }
        token.revoke(clock.instant());
        repository.save(token);
        return new Rotation(token.userId(), issueInFamily(token.userId(), token.familyId()));
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            token.revoke(clock.instant());
            repository.save(token);
        });
    }

    private String issueInFamily(UUID userId, UUID familyId) {
        var bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        var raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var now = clock.instant();
        var token = RefreshToken.issue(userId, hash(raw), familyId, now.plus(ttl), now);
        repository.save(token);
        return raw;
    }

    private static String hash(String raw) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
