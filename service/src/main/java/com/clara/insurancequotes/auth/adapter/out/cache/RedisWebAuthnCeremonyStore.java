package com.clara.insurancequotes.auth.adapter.out.cache;

import com.clara.insurancequotes.auth.api.exception.InvalidPasskeyException;
import com.clara.insurancequotes.auth.application.port.out.StoredCeremony;
import com.clara.insurancequotes.auth.application.port.out.WebAuthnCeremonyStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisWebAuthnCeremonyStore implements WebAuthnCeremonyStore {

    private static final String KEY_PREFIX = "auth:webauthn:ceremony:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisWebAuthnCeremonyStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(String challengeId, StoredCeremony ceremony, Duration ttl) {
        try {
            redis.opsForValue().set(key(challengeId), objectMapper.writeValueAsString(ceremony), ttl);
        } catch (JsonProcessingException exception) {
            throw new InvalidPasskeyException("could not store ceremony");
        }
    }

    @Override
    public Optional<StoredCeremony> take(String challengeId) {
        var value = redis.opsForValue().getAndDelete(key(challengeId));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, StoredCeremony.class));
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new InvalidPasskeyException("malformed ceremony");
        }
    }

    private static String key(String challengeId) {
        return KEY_PREFIX + challengeId;
    }
}
