# ADR-011: Redis for shared ephemeral state

## Status

Accepted

## Context

The service may run as multiple JVM instances or short-lived serverless instances. Instance-local Caffeine state makes a WebAuthn ceremony started by one instance unavailable to another instance and makes quote-cache hit rates dependent on request routing.

The system already has durable stores: PostgreSQL owns users, passkeys, refresh-token rotation, and quote aggregates; Kafka-backed application flows own business events. Those responsibilities must not move to a cache.

## Decision

Use Redis as shared ephemeral infrastructure for both current Caffeine use cases:

- a ten-minute, JSON-serialized quote cache shared by service instances;
- a five-minute WebAuthn registration/assertion ceremony store behind the `WebAuthnCeremonyStore` outbound port.

Ceremony retrieval uses Redis `GETDEL` through Spring Data Redis `ValueOperations.getAndDelete`, so a ceremony is consumed once without a distributed lock. Redis keys are namespaced under `auth:webauthn:ceremony:` and all values have bounded TTLs.

Quote-cache failures are logged and fail open to PostgreSQL because the cache is an optimization. WebAuthn ceremony-store failures fail closed through the existing passkey error because continuing without the challenge would be unsafe. Redis is not used as a source of truth, transaction coordinator, or general-purpose distributed lock service.

## Consequences

Positive:

- horizontally scaled and serverless instances share the short-lived state that must cross requests;
- no sticky sessions are required for WebAuthn ceremonies;
- quote caching remains transparent to the domain and application services;
- TTLs bound stale/abandoned state.

Costs and operations:

- local and deployed stacks must run Redis and expose its health status;
- Redis availability must be monitored separately from PostgreSQL;
- quote reads remain available during Redis outages, but cache effectiveness is reduced;
- users must restart a WebAuthn ceremony when Redis is unavailable or the ceremony expires.

## Rejected alternatives

- **Keep Caffeine:** does not work across instances or serverless invocations.
- **Persist ceremonies in PostgreSQL:** adds durable cleanup and sensitive transient-state persistence for data that only needs bounded-lived coordination.
- **Use distributed locks:** unnecessary for one-time consumption because `GETDEL` provides the required atomic read-and-delete operation.
- **Use Redis for durable business state:** would duplicate or replace PostgreSQL invariants and make cache availability a correctness dependency for unrelated domain operations.
