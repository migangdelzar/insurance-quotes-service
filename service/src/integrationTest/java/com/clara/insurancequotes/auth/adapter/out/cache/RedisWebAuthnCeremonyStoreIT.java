package com.clara.insurancequotes.auth.adapter.out.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.auth.application.port.out.StoredCeremony;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class RedisWebAuthnCeremonyStoreIT {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redis;
    private RedisWebAuthnCeremonyStore store;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
        connectionFactory.afterPropertiesSet();
        redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
        store = new RedisWebAuthnCeremonyStore(redis, new ObjectMapper().findAndRegisterModules());
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void storesJsonWithTtlAndConsumesTheCeremonyOnlyOnce() {
        var ceremony = new StoredCeremony(StoredCeremony.CeremonyType.REGISTRATION, "creation-json");

        store.save("challenge-1", ceremony, Duration.ofSeconds(30));

        assertThat(redis.getExpire("auth:webauthn:ceremony:challenge-1")).isPositive();
        assertThat(store.take("challenge-1")).contains(ceremony);
        assertThat(store.take("challenge-1")).isEmpty();
    }
}
