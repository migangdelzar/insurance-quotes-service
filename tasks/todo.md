# Backend core refactor checkpoint

- [x] Standardize modules to DDD + Hexagonal Modulith boundaries.
- [x] Separate API contracts, application services, domain models, and directional adapters.
- [x] Keep domain exceptions transport-neutral and map them at inbound web boundaries.
- [x] Apply Lombok constructor/logging conventions without changing domain behavior.
- [x] Verify unit, integration, Modulith, Docker image, and Compose smoke-test behavior.

## Results

The backend now follows the responsibility-based package structure defined by the module templates. Quote lookup exceptions live in the application layer, domain rules remain framework-neutral, and HTTP error translation is isolated in inbound adapters.

## Redis, responsive UI, and dynamic locale checkpoint

- [x] Reconcile the written implementation plan with the completed backend and frontend changes.
- [x] Verify backend unit, integration, formatting, and coverage checks.
- [x] Verify frontend unit tests, build, lint, and targeted responsive browser checks.
- [x] Validate Compose configuration and Redis health.
- [x] Commit and push all intended backend changes.
- [x] Commit and push all intended frontend changes.
- [x] Verify remote branch tips and clean working trees.

### Acceptance criteria

- Redis is the shared, ephemeral store for quote caching and one-time WebAuthn ceremonies.
- Browser locale detection is normalized and propagated as `Accept-Language`.
- Responsive checks pass at 320, 375, 768, and 1280 pixels without horizontal overflow.
- Verification limitations are recorded in the implementation plan.

### Working notes

- PostgreSQL remains the durable source of truth.
- The existing full E2E journey suite needs a clean demo database because the shared database is already passkey/MFA-mutated.
- Docker image rebuild is environment-limited by the local Buildx/Maven build stage; Maven and Compose checks are available.

## GitHub Actions CI checkpoint

- [x] Add backend quality and Docker workflow.
- [x] Add backend full-stack JVM smoke workflow.
- [x] Add frontend quality and responsive-browser workflow.
- [x] Verify cancellation groups and workflow syntax.
- [x] Commit and push workflow changes.

### Final verification notes

- The API CORS defaults and JVM Compose environment include the insurance web port `3100`, covered by a MockMvc preflight regression test.
- Local E2E verification required resetting only the demo user's passkeys and refresh tokens; the durable user and quote records were preserved.
- The API image was rebuilt after the Redis Java-time serializer fix so the running Docker stack matched source.
- `QuoteView` now copies health-condition collections before exposing them to API responses and Redis cache values, preventing detached Hibernate collections from escaping the persistence boundary.

### Observability verification

- [x] Expose `/actuator/prometheus` to the internal scraper while keeping the rest of the API security boundary protected.
- [x] Validate Prometheus target health and an `up{job="insurance-quotes-api"}` sample.
- [x] Validate Grafana's provisioned Prometheus datasource and six-panel dashboard.
- [x] Correct the dashboard JSON syntax error that prevented Grafana provisioning.

## Default demo users checkpoint

- [x] Add a configurable, idempotent seed for three local password users.
- [x] Preserve the existing `demo` credentials and add `demo-two` and `demo-three`.
- [x] Add TDD coverage for seeding all users and skipping existing users.
- [x] Document credentials and passkey/MFA behavior.

## Metrics, Kafka, and full-stack CI checkpoint

- [x] Document Micrometer → Prometheus → Grafana ownership for operational metrics.
- [x] Document Kafka's boundary as durable domain-event transport, not a Prometheus metric aggregator.
- [x] Preserve synchronous coverage pricing because the wizard needs the result immediately.
- [x] Validate the same-origin Nginx `/api/actuator/health` proxy in the full-stack GitHub Actions smoke workflow.
- [x] Keep push and pull-request workflows canceling superseded runs with `cancel-in-progress: true`.

## Observability and mobile wizard polish checkpoint

- [x] Add bounded business metrics for quote lifecycle, pricing, insurer calls, cache failures, domain events, and rate limits.
- [x] Add Redis-backed distributed rate limiting with atomic fixed windows, 429 headers, and fail-open telemetry.
- [x] Add Spring Boot OTLP tracing, trace/span log MDC fields, and the Okio runtime compatibility pin.
- [x] Add Loki, Tempo, and Alloy to the observability Compose overlay with persistent local volumes.
- [x] Provision Prometheus, Loki, and Tempo in Grafana with trace-to-log correlation and an operations dashboard.
- [x] Add CI Compose validation for the complete observability overlay.
- [x] Polish wizard progress and mobile action placement for small screens with safe-area padding.
- [x] Verify web unit/build checks and Playwright mobile/desktop responsive flows.

### Verification evidence

- Backend: `mise exec -- mvn -B -pl service test` — 69 tests passed.
- Frontend: `bun run --filter web test --run` — 58 tests passed; production build passed.
- Browser: Playwright mobile premium journey passed with the action dock computed as `position: fixed`; 11 responsive desktop checks passed.
- Live telemetry: Prometheus ready, Loki `/ready`, Tempo `/ready`, API health, rate-limit samples, Tempo trace search, and Loki range query all returned successfully.
