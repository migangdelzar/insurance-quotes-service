package com.clara.insurancequotes.shared.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.clara.insurancequotes.config.BusinessMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class RedisCacheErrorHandlerTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final RedisCacheErrorHandler handler = new RedisCacheErrorHandler(new BusinessMetrics(registry));

    @Test
    void cacheFailuresAreBestEffortAndDoNotBreakTheDatabasePath() {
        assertThatCode(() ->
                        handler.handleCacheGetError(new IllegalStateException("redis unavailable"), null, "quote-1"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCachePutError(
                        new IllegalStateException("redis unavailable"), null, "quote-1", null))
                .doesNotThrowAnyException();
        assertThatCode(() ->
                        handler.handleCacheEvictError(new IllegalStateException("redis unavailable"), null, "quote-1"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheClearError(new IllegalStateException("redis unavailable"), null))
                .doesNotThrowAnyException();
        assertThat(registry.get("cache.errors")
                        .tag("operation", "get")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.get("cache.errors")
                        .tag("operation", "put")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.get("cache.errors")
                        .tag("operation", "evict")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.get("cache.errors")
                        .tag("operation", "clear")
                        .counter()
                        .count())
                .isEqualTo(1);
    }
}
