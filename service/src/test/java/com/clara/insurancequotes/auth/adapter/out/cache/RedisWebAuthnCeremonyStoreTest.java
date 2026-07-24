package com.clara.insurancequotes.auth.adapter.out.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clara.insurancequotes.auth.application.port.out.StoredCeremony;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisWebAuthnCeremonyStoreTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private RedisWebAuthnCeremonyStore store;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        store = new RedisWebAuthnCeremonyStore(redis, new ObjectMapper());
    }

    @Test
    void save_serializesCeremonyWithTtlAndTakeConsumesIt() {
        var ceremony = new StoredCeremony(StoredCeremony.CeremonyType.ASSERTION, "request-json");
        var ttl = Duration.ofMinutes(5);
        when(values.getAndDelete("auth:webauthn:ceremony:challenge-1"))
                .thenReturn("{\"type\":\"ASSERTION\",\"payload\":\"request-json\"}");

        store.save("challenge-1", ceremony, ttl);

        var taken = store.take("challenge-1");

        verify(values)
                .set(
                        eq("auth:webauthn:ceremony:challenge-1"),
                        eq("{\"type\":\"ASSERTION\",\"payload\":\"request-json\"}"),
                        eq(ttl));
        verify(values).getAndDelete("auth:webauthn:ceremony:challenge-1");
        assertThat(taken).contains(ceremony);
    }

    @Test
    void take_returnsEmptyWhenCeremonyIsMissing() {
        when(values.getAndDelete("auth:webauthn:ceremony:missing")).thenReturn(null);

        assertThat(store.take("missing")).isEmpty();
    }
}
