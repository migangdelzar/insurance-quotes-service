package com.clara.insurancequotes.shared.ratelimit;

import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisRateLimiter implements RateLimiter {

    private static final String SCRIPT =
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            local ttl = redis.call('PTTL', KEYS[1])
            return { current, ttl }
            """;

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final RedisScript<List<Long>> INCREMENT_SCRIPT = new DefaultRedisScript(SCRIPT, List.class);

    private final StringRedisTemplate redis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public RateLimitDecision tryAcquire(String bucket, String clientKey, long limit, Duration window) {
        if (limit < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate-limit limit and window must be positive");
        }

        var key = "rate-limit:%s:%s".formatted(bucket, clientKey);
        var result = redis.execute(INCREMENT_SCRIPT, List.of(key), String.valueOf(window.toMillis()));
        if (result == null || result.size() < 2 || result.get(0) == null || result.get(1) == null) {
            throw new IllegalStateException("Redis rate-limit script returned no decision");
        }

        var current = result.get(0);
        var ttl = Duration.ofMillis(Math.max(0, result.get(1)));
        return new RateLimitDecision(current <= limit, limit, Math.max(0, limit - current), ttl);
    }
}
