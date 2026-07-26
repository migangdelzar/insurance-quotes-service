package com.clara.insurancequotes.shared.ratelimit;

import java.time.Duration;

public interface RateLimiter {

    RateLimitDecision tryAcquire(String bucket, String clientKey, long limit, Duration window);
}
