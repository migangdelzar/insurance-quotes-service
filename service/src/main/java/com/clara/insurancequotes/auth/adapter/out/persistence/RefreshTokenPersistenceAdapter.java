package com.clara.insurancequotes.auth.adapter.out.persistence;

import com.clara.insurancequotes.auth.application.port.out.RefreshTokenRepository;
import com.clara.insurancequotes.auth.domain.model.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Implements refresh-token persistence with Spring Data JPA. */
@Repository
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository delegate;

    @Override
    public RefreshToken save(RefreshToken token) {
        return delegate.save(token);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return delegate.findByTokenHash(tokenHash);
    }

    @Override
    public int revokeIfActive(UUID tokenId, Instant now) {
        return delegate.revokeIfActive(tokenId, now);
    }

    @Override
    public int revokeFamily(UUID familyId, Instant now) {
        return delegate.revokeFamily(familyId, now);
    }
}
