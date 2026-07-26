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

- [x] Write failing tests for bounded business meters and the atomic limiter decision contract.
- [x] Run the focused tests; expect failure because the new meters and limiter do not exist.
- [x] Implement the minimum Micrometer meters and Redis fixed-window limiter with structured 429 responses and headers.
- [x] Register the interceptor and wire quote/auth mutation buckets through configuration.
- [x] Run focused tests and the backend unit suite; expect all tests to pass.
- [x] Commit `feat(observability): add business metrics and redis rate limiting`.

### Task 2: Application trace export and correlated Docker logs

**Files:**
- Modify: `service/pom.xml`
- Modify: `service/src/main/resources/application.yml`
- Modify: `service/src/main/resources/application-docker.yml`
- Modify: `service/src/main/resources/logback-spring.xml`
- Test: `service/src/test/java/com/clara/insurancequotes/config/ObservabilityConfigurationTest.java`

- [x] Validate the tracing sampling property and Docker OTLP endpoint against the running API configuration.
- [x] Add `spring-boot-starter-opentelemetry`, configure OTLP/HTTP sampling and endpoint, and ensure Docker JSON logs include trace/span MDC fields.
- [x] Run `mvn -pl service test`; all backend tests pass.
- [x] Verify a real trace in Tempo and API logs in Loki.

### Task 3: Loki, Tempo, and Alloy Compose services

**Files:**
- Create: `deployment/compose/observability/loki-config.yml`
- Create: `deployment/compose/observability/tempo-config.yml`
- Create: `deployment/compose/observability/alloy-config.alloy`
- Modify: `deployment/compose/compose.observability.yml`

- [x] Add Loki single-binary filesystem/TSDB configuration and host-verified readiness.
- [x] Add Tempo local filesystem configuration with OTLP gRPC/HTTP receivers bound to `0.0.0.0` and host-verified readiness.
- [x] Add Alloy Docker discovery, `loki.source.docker`, and Loki push configuration.
- [x] Add persistent named volumes and startup ordering for minimal observability images.
- [x] Run the complete observability Compose config validation successfully.

### Task 4: Grafana data sources and correlated dashboard

**Files:**
- Modify: `deployment/compose/observability/grafana/provisioning/datasources/datasource.yml`
- Modify: `deployment/compose/observability/grafana/dashboards/quotes.json`
- Test: `service/src/test/java/com/clara/insurancequotes/config/ObservabilityDashboardTest.java`

- [x] Validate provisioning requirements for Prometheus, Loki, and Tempo UIDs plus trace-ID correlation configuration.
- [x] Provision all three data sources and add panels for request rate/error rate, quote lifecycle, cache behavior, rate limits, JVM health, and LogQL logs.
- [x] Run JSON validation and verify the live data sources through their HTTP endpoints.

### Task 5: Live stack verification and documentation

**Files:**
- Modify: `README.md`
- Modify: `tasks/todo.md`
- Modify: `tasks/lessons.md`
- Test: Compose and curl verification commands

- [x] Start the JVM observability stack with the equivalent Compose command.
- [x] Verify API `/actuator/prometheus`, Prometheus readiness, Loki `/ready`, Tempo `/ready`, rate-limit metrics, and a real trace/log after a request.
- [x] Document ports, credentials, retention limits, and the production Docker-socket collector caveat.
- [x] Run the full backend verification and record the evidence in `tasks/todo.md`.
