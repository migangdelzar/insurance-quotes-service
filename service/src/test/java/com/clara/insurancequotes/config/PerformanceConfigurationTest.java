package com.clara.insurancequotes.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

class PerformanceConfigurationTest {

    @Test
    void definesBoundedDatabaseAndTomcatRuntimeDefaults() {
        var properties = applicationProperties("application.yml");

        assertThat(properties)
                .containsEntry("spring.datasource.hikari.maximum-pool-size", "${DB_POOL_MAX_SIZE:10}")
                .containsEntry("spring.datasource.hikari.minimum-idle", "${DB_POOL_MIN_IDLE:2}")
                .containsEntry("spring.datasource.hikari.connection-timeout", "${DB_POOL_CONNECTION_TIMEOUT:5000}")
                .containsEntry("server.tomcat.threads.max", "${SERVER_TOMCAT_THREADS_MAX:200}")
                .containsEntry("server.tomcat.threads.max-queue-capacity", "${SERVER_TOMCAT_THREADS_MAX_QUEUE:100}")
                .containsEntry("server.tomcat.max-connections", "${SERVER_TOMCAT_MAX_CONNECTIONS:512}")
                .containsEntry("server.tomcat.accept-count", "${SERVER_TOMCAT_ACCEPT_COUNT:100}")
                .containsEntry("server.compression.enabled", true)
                .containsEntry("server.shutdown", "graceful")
                .containsEntry("spring.lifecycle.timeout-per-shutdown-phase", "${SERVER_SHUTDOWN_TIMEOUT:20s}");
    }

    @Test
    void demoProfileUsesLowFootprintRuntimeDefaults() {
        var properties = applicationProperties("application-demo.yml");

        assertThat(properties)
                .containsEntry("spring.datasource.hikari.maximum-pool-size", "${DB_POOL_MAX_SIZE:5}")
                .containsEntry("spring.datasource.hikari.minimum-idle", "${DB_POOL_MIN_IDLE:1}")
                .containsEntry("spring.datasource.hikari.keepalive-time", "${DB_POOL_KEEPALIVE:0}")
                .containsEntry("server.tomcat.threads.max", "${SERVER_TOMCAT_THREADS_MAX:32}")
                .containsEntry("server.tomcat.threads.max-queue-capacity", "${SERVER_TOMCAT_THREADS_MAX_QUEUE:25}")
                .containsEntry("server.tomcat.max-connections", "${SERVER_TOMCAT_MAX_CONNECTIONS:64}")
                .containsEntry("spring.lifecycle.timeout-per-shutdown-phase", "${SERVER_SHUTDOWN_TIMEOUT:10s}");
    }

    @Test
    void dockerProfileDoesNotExportTracesUnlessObservabilityIsEnabled() {
        var defaults = dockerEnvironment(Map.of());
        var observability = dockerEnvironment(Map.of("OTEL_SDK_ENABLED", "true"));

        assertThat(defaults.getProperty("management.opentelemetry.enabled", Boolean.class))
                .isFalse();
        assertThat(defaults.getProperty("management.otlp.metrics.export.enabled", Boolean.class))
                .isFalse();
        assertThat(observability.getProperty("management.opentelemetry.enabled", Boolean.class))
                .isTrue();
        assertThat(defaults.getProperty("management.opentelemetry.tracing.export.otlp.endpoint"))
                .isEqualTo("http://tempo:4318/v1/traces");
    }

    private static ConfigurableEnvironment dockerEnvironment(Map<String, Object> overrides) {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", overrides));
        var loader = new YamlPropertySourceLoader();
        var resource = new ClassPathResource("application-docker.yml");
        try {
            loader.load("application-docker", resource).forEach(environment.getPropertySources()::addLast);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load Docker profile", exception);
        }
        return environment;
    }

    private static Properties applicationProperties(String resourceName) {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(resourceName));
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
