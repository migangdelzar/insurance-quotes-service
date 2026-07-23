package com.clara.insurancequotes.auth.application.port.out;

import com.clara.insurancequotes.auth.domain.model.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    int revokeFamily(UUID familyId, Instant now);
}
