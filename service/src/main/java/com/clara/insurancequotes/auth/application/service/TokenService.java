package com.clara.insurancequotes.auth.application.service;

import com.clara.insurancequotes.auth.domain.model.User;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final Duration ttl;

    public TokenService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, @Value("${auth.jwt.ttl}") Duration ttl) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.ttl = ttl;
    }

    public record IssuedAccess(String accessToken, long expiresInSeconds) {}

    public IssuedAccess issueApiToken(User user) {
        return issue(user.username(), "api", ttl, user);
    }

    public String issueMfaToken(String username) {
        return issue(username, "mfa-pending", Duration.ofMinutes(5), null).accessToken();
    }

    public String scopeOf(String token) {
        try {
            return jwtDecoder.decode(token).getClaimAsString("scope");
        } catch (JwtException exception) {
            throw new com.clara.insurancequotes.auth.api.exception.InvalidCredentialsException();
        }
    }

    private IssuedAccess issue(String username, String scope, Duration tokenTtl, User user) {
        var now = Instant.now();
        var claimsBuilder = JwtClaimsSet.builder()
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(tokenTtl))
                .claim("scope", scope);
        if (user != null) {
            claimsBuilder.claim("uid", user.id().toString()).claim("role", user.role().name());
        }
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var token = jwtEncoder.encode(JwtEncoderParameters.from(header, claimsBuilder.build()));
        log.debug("Issued JWT for {}", username);
        return new IssuedAccess(token.getTokenValue(), tokenTtl.toSeconds());
    }
}
