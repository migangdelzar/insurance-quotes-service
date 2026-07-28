# Architecture decision flow

This map shows the primary dependency and responsibility flow. It is not a
runtime call graph: it documents ownership boundaries.

~~~mermaid
flowchart LR
  modules[ADR-001 Modulith boundaries] --> auth[ADR-002 Auth]
  modules --> persistence[ADR-003 PostgreSQL and Flyway]
  persistence --> domain[ADR-004 Quote state machine]
  auth --> redis[ADR-005 Redis ephemeral state]
  domain --> submission[ADR-006 Submission boundary]
  submission --> outbox[ADR-007 Outbox to Kafka]
  redis --> limiter[ADR-008 Redis rate limiting]
  modules --> observability[ADR-009 Observability]
  modules --> native[ADR-010 Optional native runtime]
  modules --> libraries[ADR-011 Extracted libraries]
~~~

## Ownership boundaries

| Concern | Owner | Not owned by |
|---|---|---|
| Durable users, passkeys, refresh state, quotes, migrations | PostgreSQL/Flyway | Redis or Kafka |
| Quote cache and WebAuthn challenges | Redis with bounded TTLs | PostgreSQL business invariants |
| Quote-submitted business events | Spring Modulith outbox to Kafka | Prometheus metrics |
| Request and business metrics | Actuator/Micrometer → Prometheus → Grafana | Kafka |
| Structured logs | API stdout → Alloy → Loki | Redis |
| Distributed traces | OpenTelemetry → Tempo → Grafana | Kafka |
| Request protection | Redis fixed-window Lua buckets | Durable quote state |
| Application runtime | Java 17 JVM by default | Java 25 runtime |

The key separation is intentional: Kafka transports durable business facts,
while Actuator/Micrometer exposes operational measurements. Redis coordinates
bounded ephemeral decisions; PostgreSQL remains the source of truth.

## Historical transition

The original Caffeine record was superseded when the application gained
multi-instance/serverless requirements and moved both quote caching and
WebAuthn ceremony state to Redis. The historical record remains in
archive/ADR-003-caffeine-cache-superseded.md.
