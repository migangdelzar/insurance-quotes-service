# ADR-013: Use Redis for shared HTTP rate limiting

## Status

Accepted

## Context

The API can run as multiple stateless instances behind the same-origin web
proxy. An in-memory limiter would allow a caller to bypass limits by reaching a
different instance, and a database-backed limiter would add unnecessary write
load to PostgreSQL.

## Decision

Use Redis as the shared rate-limit state store. A Lua script atomically
increments a namespaced fixed-window counter and assigns its expiry on the
first request. The HTTP interceptor applies stricter buckets to authentication
endpoints and bounded mutation buckets to quote creation, coverage updates,
and submission.

The limiter returns standard response metadata:

- `X-RateLimit-Limit`
- `X-RateLimit-Remaining`
- `Retry-After` when the request is rejected with `429 RATE_LIMITED`

Authentication buckets use the client address. Authenticated quote mutations
use the JWT subject; unauthenticated requests use the client address. The
deployment must only trust forwarded client-address headers from a configured
reverse proxy.

Redis failures fail open and emit a metric/log warning. This preserves API
availability while making an infrastructure outage visible; production may
switch authentication buckets to fail-closed if the threat model requires it.

Rate-limit keys are never exported as metric labels and never contain
credentials, tokens, quote IDs, or email addresses.

## Consequences

### Positive

- Limits are consistent across horizontally scaled API instances.
- Redis TTLs automatically remove expired buckets.
- PostgreSQL remains focused on durable business state.

### Negative

- Redis becomes part of the request path for protected buckets.
- Fixed windows allow boundary bursts; a token bucket can be introduced later
  if the product needs smoother traffic shaping.

## Related decisions

- [ADR-011: Redis for shared ephemeral state](ADR-011-redis-shared-ephemeral-state.md)
- [ADR-012: Operational metrics and Kafka boundary](ADR-012-operational-metrics-and-kafka-boundary.md)
