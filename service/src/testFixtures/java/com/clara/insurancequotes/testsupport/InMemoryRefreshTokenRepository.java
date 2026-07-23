package com.clara.insurancequotes.testsupport;

import com.clara.insurancequotes.auth.application.port.out.RefreshTokenRepository;
import com.clara.insurancequotes.auth.domain.model.RefreshToken;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

    private final Map<String, RefreshToken> byHash = new ConcurrentHashMap<>();

    @Override
    public RefreshToken save(RefreshToken token) {
        byHash.put(token.tokenHash(), token);
        return token;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(byHash.get(tokenHash));
    }

    @Override
    public int revokeFamily(UUID familyId, Instant now) {
        var count = 0;
        for (var token : byHash.values()) {
            if (token.familyId().equals(familyId) && !token.isRevoked()) {
                token.revoke(now);
                count++;
            }
        }
        return count;
    }
}
