package com.clara.insurancequotes.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

public final class Containers {

    public static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine").withDatabaseName("quotes");

    public static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.8.1");

    public static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private Containers() {}

    public static void registerPostgres(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    public static void registerKafka(DynamicPropertyRegistry registry) {
        KAFKA.start();
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    public static void registerRedis(DynamicPropertyRegistry registry) {
        REDIS.start();
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
