package com.clara.insurancequotes.shared.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterTest {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Mock
    private StringRedisTemplate redis;

    @Test
    void allowsRequestAndReturnsRemainingBudget() {
        when(redis.execute(any(RedisScript.class), eq(List.of("rate-limit:auth:client-1")), eq("60000")))
                .thenReturn(List.of(1L, 60000L));

        var decision = new RedisRateLimiter(redis).tryAcquire("auth", "client-1", 5, WINDOW);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remaining()).isEqualTo(4);
        assertThat(decision.retryAfter()).isEqualTo(Duration.ofMillis(60000));
        verify(redis).execute(any(RedisScript.class), eq(List.of("rate-limit:auth:client-1")), eq("60000"));
    }

    @Test
    void rejectsRequestWhenLimitIsReached() {
        when(redis.execute(any(RedisScript.class), eq(List.of("rate-limit:quote_mutation:client-1")), eq("60000")))
                .thenReturn(List.of(6L, 42000L));

        var decision = new RedisRateLimiter(redis).tryAcquire("quote_mutation", "client-1", 5, WINDOW);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.remaining()).isZero();
        assertThat(decision.retryAfter()).isEqualTo(Duration.ofMillis(42000));
    }
}
