# ADR-009: Actuator/Micrometer and OpenTelemetry observability stack

## Status

Accepted

## Date

2026-07-28

## Decision

Use Spring Boot Actuator and Micrometer as the API metrics path, expose Prometheus format at \`/actuator/prometheus\`, and visualize it in Grafana. Export sampled OpenTelemetry traces to Tempo. Emit structured JSON logs to stdout and collect them with Grafana Alloy into Loki. Grafana provisions the Prometheus, Tempo, and Loki data sources and the quote dashboard.

Business metrics remain low-cardinality meters. Kafka remains the durable \`QuoteSubmitted\` event transport and is not used to push operational metrics.

## Context and decision drivers

The service needs actionable signals for request failures, quote lifecycle, insurer latency, cache failures, rate limiting, correlation, and distributed request tracing without making the application depend on Grafana or a custom metrics broker.

## Considered alternatives

- **Application-specific metrics endpoint:** rejected because Actuator and Micrometer provide standard meters and Prometheus exposition.
- **Kafka as metrics pipeline:** rejected because dashboards need scrape/query semantics and metrics should not become durable business events.
- **Only logs:** rejected because counters, rates, and latency percentiles are difficult to query reliably from logs alone.
- **Zipkin-only tracing:** rejected in favor of the OpenTelemetry/Tempo path already used by the Docker observability overlay.

## Implementation evidence

- \`config/BusinessMetrics.java\`
- \`config/CorrelationIdFilter.java\`
- \`service/src/main/resources/logback-spring.xml\`
- \`deployment/compose/compose.observability.yml\`
- \`deployment/compose/observability/\`
- \`deployment/compose/observability/grafana/dashboards/quotes.json\`

## Consequences

### Positive

- Metrics, logs, and traces have clear ownership and standard query paths.
- Correlation IDs and trace links reduce investigation time.
- The stack is reproducible locally and can be replaced by managed services in deployment without changing business code.

### Negative and operational

- The local stack adds Prometheus, Grafana, Loki, Tempo, and Alloy containers.
- Cardinality discipline and retention policies are operational requirements.
- Docker log collection is a development/deployment choice; production may use a platform-native collector.

## Related decisions

- [ADR-007: Outbox events](ADR-007-outbox-kafka-events.md)
- [ADR-008: Redis rate limiting](ADR-008-redis-rate-limiting.md)
