package com.clara.insurancequotes.auth.application.service;

import com.clara.insurancequotes.auth.api.model.TokenResponse;
import com.clara.insurancequotes.auth.application.exception.InvalidCredentialsException;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final Duration ttl;
    private final String demoUsername;
    private final String demoPassword;

    public TokenService(
            JwtEncoder jwtEncoder,
            @Value("${auth.jwt.ttl}") Duration ttl,
            @Value("${auth.demo.username}") String demoUsername,
            @Value("${auth.demo.password}") String demoPassword) {
        this.jwtEncoder = jwtEncoder;
        this.ttl = ttl;
        this.demoUsername = demoUsername;
        this.demoPassword = demoPassword;
    }

    public TokenResponse issueFor(String username, String password) {
        if (!demoUsername.equals(username) || !demoPassword.equals(password)) {
            throw new InvalidCredentialsException();
        }
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("scope", "api")
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
        log.debug("Issued JWT for {}", username);
        return new TokenResponse(token.getTokenValue(), ttl.toSeconds());
    }
}
