package com.clara.insurancequotes.auth.configuration;

import com.clara.insurancequotes.auth.adapter.out.webauthn.YubicoCredentialRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import java.time.Duration;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebAuthnConfig {

    @Bean
    public RelyingParty relyingParty(
            YubicoCredentialRepository credentialRepository,
            @Value("${auth.webauthn.rp-id:localhost}") String rpId,
            @Value("${auth.webauthn.origins:http://localhost:5173,http://localhost:3000}") Set<String> origins) {
        var identity =
                RelyingPartyIdentity.builder().id(rpId).name("Insurance Quotes").build();
        return RelyingParty.builder()
                .identity(identity)
                .credentialRepository(credentialRepository)
                .origins(origins)
                .allowOriginPort(true)
                .build();
    }

    @Bean
    public Cache<String, Object> webAuthnCeremonyCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(1000)
                .build();
    }
}
