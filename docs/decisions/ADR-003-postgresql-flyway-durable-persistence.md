# ADR-003: PostgreSQL with Flyway for durable persistence

## Status

Accepted

## Date

2026-07-28

## Decision

Use PostgreSQL as the durable source of truth for users, passkeys, refresh-token rotation, quotes, and business event publications. Evolve its schema with ordered, reviewable Flyway SQL migrations and validate the schema through JPA.

## Context and decision drivers

Quote lifecycle transitions, authentication state, and event handoff require ACID writes, relational constraints, reproducible container startup, and a schema history that can be reviewed with the code.

## Considered alternatives

- **Redis as the primary store:** rejected because transient availability and TTL semantics do not replace durable business invariants.
- **Document database:** rejected because quotes, users, passkeys, refresh families, and event publications have relational ownership and constraints.
- **Liquibase:** rejected because the selected SQL migration surface is smaller and easier to inspect for this service.
- **Hibernate schema generation:** rejected because production schema changes must be explicit and versioned rather than inferred at startup.

## Implementation evidence

- \`service/src/main/resources/db/migration/\`
- \`service/src/main/resources/application.yml\`
- \`service/src/main/java/com/clara/insurancequotes/*/adapter/out/persistence/\`
- \`database/init/01-init.sql\`
- \`service/src/integrationTest/\`

## Consequences

### Positive

- Durable state and schema evolution are explicit and reproducible.
- PostgreSQL transactions protect aggregate and event-publication invariants.
- The same database model works for local Compose and a managed deployment.

### Negative and operational

- PostgreSQL is required for the full application path.
- Migration ordering and backward compatibility must be reviewed before rollout.
- Backups, credentials, connection pooling, and migration execution belong to deployment operations.

## Related decisions

- [ADR-004: Quote state machine](ADR-004-data-driven-quote-state-machine.md)
- [ADR-007: Outbox events](ADR-007-outbox-kafka-events.md)
- [ADR-005: Redis ephemeral state](ADR-005-redis-shared-ephemeral-state.md)
