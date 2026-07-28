# ADR-001: DDD and hexagonal Spring Modulith package boundaries

## Status

Accepted

## Date

2026-07-28

## Decision

Implement the backend as one Spring Boot deployment composed of responsibility-based Spring Modulith modules. Each business module follows DDD and hexagonal boundaries: public \`api\`, use-case orchestration in \`application\`, business invariants in \`domain\`, and technology integrations in \`adapter\` packages.

The current modules are \`quote\`, \`auth\`, \`pricing\`, \`submission\`, \`shared\`, and \`config\`. \`ApplicationModules\` tests enforce the allowed dependency graph.

## Context and decision drivers

The challenge needs a deployable service without losing the seams required for independent business reasoning, testability, and a later distributed deployment. The domain must not depend on HTTP status types, persistence adapters, Redis, or Kafka.

## Considered alternatives

- **Flat packages by file type:** rejected because they hide ownership and make domain, transport, and infrastructure dependencies easy to mix.
- **Separate microservices per capability:** rejected because the current deployment and data volume do not justify distributed operational overhead.
- **Separate Maven module per capability:** rejected because package-level boundaries provide the required architecture checks with a simpler reactor.

## Implementation evidence

- \`service/src/main/java/com/clara/insurancequotes/{quote,auth,pricing,submission}\`
- \`service/src/test/java/com/clara/insurancequotes/ModularityTest.java\`
- \`docs/architecture/modules/\`
- \`docs/superpowers/specs/2026-07-22-package-structure-design.md\`

## Consequences

### Positive

- Domain rules remain transport-neutral and unit-testable.
- Inbound and outbound adapters are replaceable through application ports.
- Spring Modulith makes illegal module dependencies fail during verification.

### Negative and operational

- Developers must place new types by responsibility rather than convenience.
- The single deployable still shares process and database failure domains.
- Cross-module collaboration needs explicit public APIs or events.

## Related decisions

- [ADR-002: Authentication](ADR-002-jwt-refresh-webauthn-authentication.md)
- [ADR-003: Durable persistence](ADR-003-postgresql-flyway-durable-persistence.md)
- [ADR-007: Outbox events](ADR-007-outbox-kafka-events.md)
