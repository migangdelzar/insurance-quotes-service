# ADR-007: Outbox event publication to Kafka

## Status

Accepted

## Date

2026-07-28

## Decision

Publish durable business events through Spring Modulith’s event registry and \`@Externalized\` Kafka publication. The event-publication record is written with the local transaction and delivered to Kafka afterward.

Kafka carries business events such as \`QuoteSubmitted\`; it is not the transport for Prometheus/Micrometer metrics or dashboard time series.

## Context and decision drivers

Successful quote submission must not lose its business event if Kafka is temporarily unavailable. The durable handoff must share the transaction that changes the quote state.

## Considered alternatives

- **Plain \`KafkaTemplate\`:** rejected because a process failure between the database commit and broker publication can lose the event.
- **Spring Cloud Stream:** rejected because the service has one producer-oriented flow and does not need a binder abstraction yet.
- **Kafka as metrics pipeline:** rejected because dashboards need scrape/query semantics and metrics should not become durable business events.

## Implementation evidence

- \`submission/api/event/QuoteSubmitted.java\`
- \`service/src/main/resources/application.yml\`
- \`deployment/compose/docker-compose.yml\`
- \`service/src/integrationTest/java/com/clara/insurancequotes/submission/\`

## Consequences

### Positive

- PostgreSQL is the durable handoff when Kafka is unavailable.
- Kafka consumers can evolve independently from the quote transaction.
- The event boundary supports future aggregation without coupling the domain to a metrics backend.

### Negative and operational

- Event publication is eventually consistent with the remote broker.
- Kafka, topic retention, consumer lag, and publication retries require operational monitoring.
- Event schemas become integration contracts and must evolve compatibly.

## Related decisions

- [ADR-003: Durable persistence](ADR-003-postgresql-flyway-durable-persistence.md)
- [ADR-006: Submission boundary](ADR-006-submission-outside-transaction.md)
- [ADR-009: Observability](ADR-009-observability-stack.md)
