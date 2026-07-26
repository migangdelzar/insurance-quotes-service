# Observability Stack Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a local Prometheus, Grafana, Loki, Tempo, and Alloy stack with correlated API metrics, logs, and traces.

**Architecture:** Actuator/Micrometer remains the API metrics source. Spring Boot OpenTelemetry tracing exports OTLP to Tempo, while Alloy collects Docker JSON logs and forwards them to Loki. Grafana provisions all three data sources and links them using trace IDs.

**Tech Stack:** Java 17, Spring Boot 4.0.6, Micrometer, Spring Boot OpenTelemetry starter, Docker Compose, Prometheus, Grafana, Loki, Tempo, Grafana Alloy.

## Global Constraints

- Keep Java 17 runtime compatibility.
- Keep Kafka for durable business events, not operational metric transport.
- Keep observability services behind the existing Compose observability overlay.
- Never use quote IDs, user IDs, emails, or trace IDs as Prometheus labels.
- Keep structured logs free of passwords, tokens, and credential payloads.

### Task 1: Business metrics and Redis rate limiting

**Files:**
- Create: `service/src/main/java/com/clara/insurancequotes/config/RateLimitProperties.java`
- Create: `service/src/main/java/com/clara/insurancequotes/shared/ratelimit/RateLimiter.java`
- Create: `service/src/main/java/com/clara/insurancequotes/shared/ratelimit/RedisRateLimiter.java`
- Create: `service/src/main/java/com/clara/insurancequotes/shared/ratelimit/RateLimitInterceptor.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/config/BusinessMetrics.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/quote/application/service/QuoteService.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/submission/application/service/SubmissionService.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/shared/cache/RedisCacheErrorHandler.java`
- Modify: `service/src/main/resources/application.yml`
- Test: `service/src/test/java/com/clara/insurancequotes/config/BusinessMetricsTest.java`
- Test: `service/src/test/java/com/clara/insurancequotes/shared/ratelimit/RedisRateLimiterTest.java`
- Test: `service/src/test/java/com/clara/insurancequotes/shared/ratelimit/RateLimitInterceptorTest.java`

- [ ] Write failing tests for bounded business meters and the atomic limiter decision contract.
- [ ] Run the focused tests; expect failure because the new meters and limiter do not exist.
- [ ] Implement the minimum Micrometer meters and Redis fixed-window limiter with structured 429 responses and headers.
- [ ] Register the interceptor and wire quote/auth mutation buckets through configuration.
- [ ] Run focused tests and the backend unit suite; expect all tests to pass.
- [ ] Commit `feat(observability): add business metrics and redis rate limiting`.

### Task 2: Application trace export and correlated Docker logs

**Files:**
- Modify: `service/pom.xml`
- Modify: `service/src/main/resources/application.yml`
- Modify: `service/src/main/resources/application-docker.yml`
- Modify: `service/src/main/resources/logback-spring.xml`
- Test: `service/src/test/java/com/clara/insurancequotes/config/ObservabilityConfigurationTest.java`

- [ ] Write a failing test asserting the tracing sampling property and Docker OTLP endpoint are represented by the application configuration contract.
- [ ] Run `mvn -pl service -Dtest=ObservabilityConfigurationTest test`; expect failure because the trace starter/configuration is absent.
- [ ] Add `spring-boot-starter-opentelemetry`, configure sampling and OTLP endpoint, and ensure Docker JSON logs include trace/span MDC fields.
- [ ] Run the focused test and `mvn -pl service test`; expect all tests to pass.
- [ ] Commit `feat(observability): export correlated api traces and logs`.

### Task 3: Loki, Tempo, and Alloy Compose services

**Files:**
- Create: `deployment/compose/observability/loki-config.yml`
- Create: `deployment/compose/observability/tempo-config.yml`
- Create: `deployment/compose/observability/alloy-config.alloy`
- Modify: `deployment/compose/compose.observability.yml`

- [ ] Add Loki single-binary filesystem/TSDB configuration and a readiness health check.
- [ ] Add Tempo local filesystem configuration with OTLP gRPC/HTTP receivers bound to `0.0.0.0` and readiness health check.
- [ ] Add Alloy Docker discovery, `loki.source.docker`, bounded relabeling, and Loki push configuration.
- [ ] Add health-gated dependencies and persistent named volumes for local telemetry data.
- [ ] Run `docker-compose -f deployment/compose/docker-compose.yml -f deployment/compose/docker-compose.jvm.yml -f deployment/compose/compose.observability.yml config`; expect valid output.
- [ ] Commit `build(observability): add loki tempo and alloy services`.

### Task 4: Grafana data sources and correlated dashboard

**Files:**
- Modify: `deployment/compose/observability/grafana/provisioning/datasources/datasource.yml`
- Modify: `deployment/compose/observability/grafana/dashboards/quotes.json`
- Test: `service/src/test/java/com/clara/insurancequotes/config/ObservabilityDashboardTest.java`

- [ ] Write a failing test that loads the provisioning files and requires Prometheus, Loki, and Tempo UIDs plus trace-ID correlation configuration.
- [ ] Run the focused test; expect failure because only Prometheus is provisioned.
- [ ] Provision all three data sources and add panels for request rate/error rate, quote lifecycle, cache behavior, Kafka producer health, JVM health, LogQL error stream, and Tempo trace search.
- [ ] Run the focused test and Grafana/JSON validation; expect all checks to pass.
- [ ] Commit `feat(observability): provision correlated grafana signals`.

### Task 5: Live stack verification and documentation

**Files:**
- Modify: `README.md`
- Modify: `tasks/todo.md`
- Modify: `tasks/lessons.md`
- Test: Compose and curl verification commands

- [ ] Start the JVM observability stack with `mise run up jvm observability` or the equivalent Compose command.
- [ ] Verify API `/actuator/prometheus`, Prometheus target health, Loki `/ready`, Tempo `/ready`, Grafana datasource health, and a real trace/log after a request.
- [ ] Document ports, credentials, retention limits, and the production Docker-socket collector caveat.
- [ ] Run the full backend verification and commit `docs(observability): document local telemetry stack`.
