package com.clara.insurancequotes.shared.ratelimit;

import java.time.Duration;

public record RateLimitDecision(boolean allowed, long limit, long remaining, Duration retryAfter) {}
