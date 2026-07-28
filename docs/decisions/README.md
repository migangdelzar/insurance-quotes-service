# Architecture Decision Records

The active catalogue is ordered by architectural impact and dependency flow:
system shape first, then security and durable state, domain behavior, external
boundaries, operations, packaging, and finally reusable build libraries.

## Active decisions

| ADR | Decision | Impact | Status |
|---|---|---:|---|
| [001](ADR-001-spring-modulith-package-boundaries.md) | DDD and hexagonal Spring Modulith package boundaries | Very high | Accepted |
| [002](ADR-002-jwt-refresh-webauthn-authentication.md) | JWT refresh rotation and WebAuthn authentication | Very high | Accepted |
| [003](ADR-003-postgresql-flyway-durable-persistence.md) | PostgreSQL with Flyway for durable persistence | Very high | Accepted |
| [004](ADR-004-data-driven-quote-state-machine.md) | Data-driven quote state machine | High | Accepted |
| [005](ADR-005-redis-shared-ephemeral-state.md) | Redis for shared ephemeral state | High | Accepted |
| [006](ADR-006-submission-outside-transaction.md) | Submission orchestration outside database transactions | High | Accepted |
| [007](ADR-007-outbox-kafka-events.md) | Outbox event publication to Kafka | High | Accepted |
| [008](ADR-008-redis-rate-limiting.md) | Redis for shared HTTP rate limiting | Medium/high | Accepted |
| [009](ADR-009-observability-stack.md) | Actuator/Micrometer and OpenTelemetry observability | Medium/high | Accepted |
| [010](ADR-010-optional-native-runtime.md) | Native image as an optional runtime profile | Medium | Accepted |
| [011](ADR-011-early-extracted-libraries.md) | Early extraction of shared reactor libraries | Medium/low | Accepted |

## Archived decisions

| Historical record | Status | Replacement |
|---|---|---|
| [Caffeine over Redis](archive/ADR-003-caffeine-cache-superseded.md) | Superseded | [ADR-005](ADR-005-redis-shared-ephemeral-state.md) |

The old Caffeine choice is retained for historical context only. It is not an
active dependency or deployment recommendation.

## Decision flow

See [decision-flow.md](decision-flow.md) for the dependency map and the
separation between durable state, ephemeral state, business events, metrics,
logs, and traces.

## Lifecycle

PROPOSED → ACCEPTED → SUPERSEDED
                     ↘ DEPRECATED

- **Proposed:** under review; not an implementation contract.
- **Accepted:** current architectural direction.
- **Superseded:** retained historical context and replaced by a newer decision.
- **Deprecated:** no longer recommended, with no replacement required.

Accepted ADRs are not rewritten to hide history. A material change creates or
updates a superseding record and the index is updated in the same change.

## How to add a decision

1. Copy the active ADR structure and use the next available number.
2. Explain context, drivers, alternatives, decision, evidence, and consequences.
3. Link related decisions and any superseded record.
4. Update this index and decision-flow.md.
5. Verify all relative links before opening a pull request.
