# ADR-005: Redis for shared ephemeral state

## Status

Accepted

## Date

2026-07-28

## Decision

Use Redis for the ten-minute quote cache and five-minute WebAuthn registration/assertion ceremony store. Both are accessed through framework abstractions or outbound ports, use bounded TTLs, and remain separate from the PostgreSQL source of truth.

WebAuthn ceremony consumption uses Redis \`GETDEL\` so a challenge is read and deleted once without introducing a distributed lock. Quote-cache failures log and fail open to PostgreSQL; ceremony-store failures fail closed and require a new ceremony.

## Context and decision drivers

The service may run as multiple JVM instances or short-lived serverless instances. Instance-local Caffeine state would make a ceremony unavailable after routing changes and make quote-cache behavior depend on stickiness.

## Considered alternatives

- **Caffeine:** superseded because it is process-local; the historical decision is retained in \`archive/\`.
- **PostgreSQL ceremonies:** rejected because short-lived challenge data does not need durable persistence and would require cleanup jobs.
- **Distributed locks:** rejected because atomic \`GETDEL\` is sufficient.
- **Redis as durable business state:** rejected because it would duplicate PostgreSQL invariants and make cache availability a correctness dependency.

## Implementation evidence

- \`quote/configuration/CacheConfig.java\`
- \`shared/cache/RedisCacheErrorHandler.java\`
- \`auth/adapter/out/cache/RedisWebAuthnCeremonyStore.java\`
- \`service/src/main/resources/application.yml\`
- \`deployment/compose/docker-compose.yml\`

## Consequences

### Positive

- Horizontal and serverless instances share the state required across requests.
- No sticky sessions or distributed locks are required for WebAuthn.
- Redis outages reduce cache effectiveness but do not make quote reads unavailable.

### Negative and operational

- Redis health must be monitored separately from PostgreSQL.
- TTLs bound stale state but do not eliminate the need to restart expired ceremonies.
- Cache serialization is an infrastructure contract and must stay compatible with API/domain view changes.

## Related decisions

- [ADR-002: Authentication](ADR-002-jwt-refresh-webauthn-authentication.md)
- [ADR-003: Durable persistence](ADR-003-postgresql-flyway-durable-persistence.md)
- [ADR-008: Redis rate limiting](ADR-008-redis-rate-limiting.md)
- [Archived Caffeine decision](archive/ADR-003-caffeine-cache-superseded.md)
