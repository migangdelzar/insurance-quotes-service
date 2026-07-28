package com.clara.insurancequotes.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.auth.domain.model.User;
import com.clara.insurancequotes.auth.domain.model.UserRole;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class TokenServiceTest {

    private static final String SECRET = "test-secret-that-is-32-bytes-long!!";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final TokenService service = newService();

    private static TokenService newService() {
        var key = new SecretKeySpec(SECRET.getBytes(), HMAC_ALGORITHM);
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        return new TokenService(encoder, decoder, Duration.ofMinutes(30));
    }

    @Test
    void issueApiToken_embedsUserIdAndRoleClaims() {
        var user = User.create("demo-admin", "hash", UserRole.ADMIN, Instant.parse("2026-07-28T10:00:00Z"));

        var issued = service.issueApiToken(user);

        var key = new SecretKeySpec(SECRET.getBytes(), HMAC_ALGORITHM);
        var decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        var decoded = decoder.decode(issued.accessToken());

        assertThat(decoded.getClaimAsString("uid")).isEqualTo(user.id().toString());
        assertThat(decoded.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(decoded.getClaimAsString("scope")).isEqualTo("api");
        assertThat(decoded.getSubject()).isEqualTo("demo-admin");
    }
}
