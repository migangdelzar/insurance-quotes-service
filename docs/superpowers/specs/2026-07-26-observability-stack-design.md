# Observability Stack Design

| Field | Detail |
|---|---|
| Date | 2026-07-26 |
| Scope | Prometheus, Grafana, Loki, Tempo, Alloy, and application trace/log correlation |
| Repository | `insurance-quotes-service` |
| Status | Approved for implementation |

## Goal

Provide one local Compose observability experience for metrics, logs, and
traces. Grafana will query Prometheus, Loki, and Tempo and expose links between
related signals without moving business metrics into Kafka.

## Decisions

- Micrometer and Actuator remain the application metrics path.
- Business meters cover quote creation, coverage updates, premium calculation,
  submissions, insurer calls, cache failures, and rate-limit outcomes using
  bounded labels only.
- Redis provides atomic, shared fixed-window rate-limit counters at the HTTP
  boundary. Authentication uses a stricter IP bucket; quote mutations use an
  authenticated subject or trusted ingress address. Redis failure fails open
  with a metric and warning so a cache outage does not become a total API
  outage.
- Spring Boot's OpenTelemetry starter exports sampled traces over OTLP to Tempo.
- The API keeps structured JSON logs on stdout; Grafana Alloy tails Docker
  container logs and forwards them to Loki.
- Grafana provisions Prometheus, Loki, and Tempo datasources and a dashboard
  with metric, log, and trace panels.
- Trace and log correlation uses `traceId`/`spanId` MDC fields. Quote IDs may
  appear in log bodies for debugging, but never as metric labels.
- The existing observability overlay owns the additional services. The base
  application stack remains usable without observability services.

## Runtime topology

```text
API --/actuator/prometheus--> Prometheus --query--> Grafana
API --OTLP/gRPC traces-----> Tempo -------------> Grafana
API --JSON stdout----------> Alloy --Loki push--> Loki ------------> Grafana
Redis <--- atomic rate-limit counters --- API
```

Tempo uses local filesystem storage for this development stack. Loki uses
single-binary TSDB/filesystem storage. Neither configuration is a production
retention or high-availability design.

## Configuration boundaries

### Metrics and rate limiting

Actuator exposes Micrometer meters through `/actuator/prometheus`; Prometheus
scrapes them directly. Kafka remains the durable transport for `QuoteSubmitted`
and its producer metrics remain platform telemetry. The API does not publish
every meter to Kafka.

Redis rate limiting uses an atomic `INCR` plus `PEXPIRE` Lua script. Responses
include `X-RateLimit-Limit`, `X-RateLimit-Remaining`, and `Retry-After` when a
request is limited. The limiter never uses user IDs, quote IDs, or credentials
as Prometheus labels.

### API

Add `spring-boot-starter-opentelemetry` and configure:

```yaml
management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING_PROBABILITY:1.0}
  opentelemetry:
    tracing:
      export:
        otlp:
          endpoint: ${OTEL_EXPORTER_OTLP_TRACES_ENDPOINT:http://localhost:4317}
```

The Docker profile overrides the endpoint to `http://tempo:4317`. Local
non-Docker execution defaults to localhost. Sampling is intentionally 100% for
the demo so a recorded journey is easy to inspect; production should lower it.

### Logs

The existing Docker Logstash encoder remains the single application logging
format. Alloy reads Docker logs through the Docker socket, adds bounded labels
(`service`, `compose_project`, and `container`), and pushes entries to Loki.
The Docker socket is a local-development trade-off and must be replaced by a
node/runtime log collector in production.

### Grafana

Provision three datasources with stable UIDs:

- `prometheus` → `http://prometheus:9090`
- `loki` → `http://loki:3100`
- `tempo` → `http://tempo:3200`

The Tempo datasource links to Loki using `traceId`, and the Loki datasource
links to Tempo using the same field. Dashboard panels use PromQL, LogQL, and
TraceQL to show the same request from different perspectives.

## Verification contract

- `docker compose config` succeeds with the observability overlay.
- Prometheus reports the API target as `up`.
- `/actuator/prometheus` contains application meters.
- Loki `/ready` returns ready and receives an API log line.
- Tempo `/ready` returns ready and contains a trace after an API request.
- Grafana provisions all three datasources and the observability dashboard.
- Existing JVM, native configuration validation, unit tests, and full-stack
  smoke checks remain green.

## References

- Spring Boot tracing and OTLP configuration:
  https://docs.spring.io/spring-boot/reference/actuator/tracing.html
- Spring Boot observability and correlation:
  https://docs.spring.io/spring-boot/reference/actuator/observability.html
- Grafana Loki Docker installation:
  https://grafana.com/docs/loki/latest/setup/install/docker/
- Grafana Alloy Docker log source:
  https://grafana.com/docs/alloy/latest/reference/components/loki/loki.source.docker/
- Grafana Tempo configuration:
  https://grafana.com/docs/tempo/latest/configuration/
