# ADR-012: Keep operational metrics in Micrometer and business events in Kafka

## Status

Accepted

## Context

The service already exposes Micrometer metrics through `/actuator/prometheus`,
Prometheus scrapes the endpoint, and Grafana reads the resulting time series.
The quote submission flow also publishes the durable `QuoteSubmitted` business
event through Spring Modulith's event publication registry to Kafka.

These channels have different responsibilities. Operational dashboards need
low-latency measurements such as request latency, submission outcomes, insurer
latency, and cache failures. Kafka needs durable business facts that can be
consumed by other modules or future analytics projections. Using Kafka as the
transport for every metric would add latency, storage, consumers, and failure
modes without improving Prometheus scraping.

## Decision drivers

- Keep dashboard metrics available while Kafka is delayed or unavailable.
- Preserve durable domain-event delivery for integrations and analytics.
- Avoid high-cardinality labels such as quote IDs, user IDs, emails, and traces.
- Keep the synchronous coverage-pricing request synchronous because its result
  is required immediately by the wizard.
- Make the local Grafana/Prometheus setup represent the production boundary.

## Considered options

### Micrometer to Prometheus, domain events to Kafka

- **Pros:** direct operational visibility, low overhead, clear ownership, and
  independent failure domains.
- **Cons:** two observability/integration mechanisms must be understood.

### Publish all metrics to Kafka and aggregate them there

- **Pros:** a single durable stream and flexible downstream aggregation.
- **Cons:** unnecessary complexity for operational metrics, delayed dashboard
  visibility, additional consumers and storage, and duplicated time-series
  functionality already provided by Prometheus.

### Calculate coverage asynchronously through Kafka

- **Pros:** potentially decouples pricing from the request path.
- **Cons:** breaks the immediate quote-selection experience and changes the API
  contract without a business requirement.

## Decision

The service will:

1. Record operational metrics with Micrometer in-process.
2. Expose them through Spring Boot Actuator's Prometheus endpoint.
3. Let Prometheus scrape the API and Grafana query Prometheus.
4. Publish durable business events, including `QuoteSubmitted`, to Kafka via
   Spring Modulith's transactional event publication mechanism.
5. Keep coverage pricing synchronous and out of Kafka.

The initial metric catalog is:

| Area | Metrics | Label guidance |
|---|---|---|
| HTTP | request count, error count, latency | use framework route/method/status labels; no IDs |
| Quote lifecycle | created, coverage updates, submissions, expirations | outcome/type only when bounded |
| Insurer | call count, success/failure, latency | bounded outcome and provider labels only |
| Cache | hit/miss, eviction, Redis errors | cache name and outcome only |
| Kafka/outbox | publication failures, delivery latency, consumer lag where applicable | topic and outcome only |
| Authentication | login success/failure and MFA outcomes | bounded outcome/reason only; never usernames |

Spring Boot's HTTP and Kafka client meters should be reused where available;
custom meters are added only for business outcomes that the framework cannot
observe. No `quoteId`, `userId`, email, trace ID, or arbitrary input is used as
a metric label.

If product analytics later needs a durable aggregation, a Kafka consumer may
build a separate projection or warehouse dataset from domain events. That
projection is not a replacement for Prometheus operational metrics and does
not participate in the synchronous quote-pricing path.

## Consequences

### Positive

- Grafana remains useful even when Kafka is unavailable.
- Kafka remains focused on durable business integration events.
- The existing six-panel dashboard can evolve incrementally from existing
  `BusinessMetrics` meters.
- The architecture scales to multiple stateless API instances because metrics
  are scraped per instance and business events use the durable publication
  registry.

### Negative

- Operational metrics and business events are queried through different tools.
- A future analytics projection will require a separate Kafka consumer and
  storage choice.

## Related decisions

- [ADR-002: Outbox event publication to Kafka](ADR-002-outbox-kafka-events.md)
- [ADR-008: Metrics and structured logs as the first observability tier](ADR-008-observability-tier.md)
- [ADR-011: Redis for shared ephemeral state](ADR-011-redis-shared-ephemeral-state.md)
