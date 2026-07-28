# ADR-008: Redis for shared HTTP rate limiting

## Status

Accepted

## Date

2026-07-28

## Decision

Use Redis as the shared state store for fixed-window HTTP rate limits. A Lua script atomically increments a namespaced bucket and assigns its expiry on the first request. Authentication endpoints receive stricter limits; quote creation, coverage updates, and submission use bounded mutation buckets.

The interceptor emits \`X-RateLimit-Limit\`, \`X-RateLimit-Remaining\`, and \`Retry-After\` for rejected requests. Redis failures fail open for availability and emit a metric/log warning. Forwarded client-address headers are trusted only when explicitly enabled for a configured reverse proxy.

## Context and decision drivers

The API can run as multiple stateless instances behind the same-origin web proxy. An in-memory limiter would allow a caller to bypass limits by reaching a different instance, while database writes would add avoidable load to PostgreSQL.

## Considered alternatives

- **In-memory counters:** rejected because limits would be instance-local.
- **PostgreSQL counters:** rejected because every protected request would add write contention to the durable store.
- **Distributed lock service:** rejected because the Lua script is sufficient for atomic bucket decisions.
- **Kafka-based throttling:** rejected because request admission needs a fast synchronous decision rather than durable event delivery.

## Implementation evidence

- \`shared/ratelimit/RedisRateLimiter.java\`
- \`shared/ratelimit/RateLimitInterceptor.java\`
- \`shared/ratelimit/RateLimitProperties.java\`
- \`config/BusinessMetrics.java\`
- \`service/src/main/resources/application.yml\`

## Consequences

### Positive

- Limits are consistent across horizontally scaled API instances.
- Redis TTLs remove expired buckets automatically.
- Keys never contain credentials, tokens, quote IDs, or email addresses.

### Negative and operational

- Redis is on the request path for protected buckets.
- Fixed windows allow boundary bursts; a token bucket can be introduced later if smoother traffic shaping is required.
- Fail-open behavior favors availability over strict protection during a Redis outage and must remain visible in alerts.

## Related decisions

- [ADR-005: Redis ephemeral state](ADR-005-redis-shared-ephemeral-state.md)
- [ADR-009: Observability](ADR-009-observability-stack.md)
