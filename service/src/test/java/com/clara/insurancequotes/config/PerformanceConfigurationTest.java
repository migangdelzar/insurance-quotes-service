package com.clara.insurancequotes.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
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
        var properties = applicationProperties("application-docker.yml");

        assertThat(properties)
                .containsEntry("management.opentelemetry.enabled", "${OTEL_SDK_ENABLED:false}")
                .containsEntry(
                        "management.opentelemetry.tracing.export.otlp.endpoint",
                        "${OTEL_EXPORTER_OTLP_TRACES_ENDPOINT:http://tempo:4318/v1/traces}");
    }

    private static Properties applicationProperties(String resourceName) {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(resourceName));
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
