package com.clara.insurancequotes.shared.cache;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class RedisCacheErrorHandlerTest {

    private final RedisCacheErrorHandler handler = new RedisCacheErrorHandler();

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
    }
}
