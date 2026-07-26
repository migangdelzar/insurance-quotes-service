# insurance-quotes-service

Spring Boot 4.0.6 / Java 17 backend for the insurance quote flow. It is a DDD and hexagonal Spring Modulith: business capabilities are package modules, domain code is transport-neutral, and inbound/outbound adapters are explicit.

Sibling frontend: [insurance-quotes-web](../insurance-quotes-web)

## Run

Install the pinned tools and hooks, then start the default JVM stack:

```bash
mise run setup
mise run up                         # PostgreSQL + Kafka + Redis + JVM API
mise run up jvm observability       # add Prometheus :9090, Grafana :3001, Loki :3101, Tempo :3200
mise run up jvm full                # add the frontend at :3100 and local insurer stub
mise run up jvm full e2e            # same stack; keeps the explicit E2E overlay alias
```

The full-stack command expects the sibling frontend directory shown above. The API is available at `http://localhost:8080`; Swagger UI is at `/swagger-ui.html`. Full local Compose runs use WireMock on `http://localhost:8089` as a deterministic insurer stand-in, so quote submission does not depend on external `httpstat.us` availability. Set `INSURER_BASE_URL` in a deployment-specific environment to use a real insurer endpoint. The local profile seeds three password users when they do not already exist:

| Username | Password |
| --- | --- |
| `demo` | `demo-password` |
| `demo-two` | `demo-password-two` |
| `demo-three` | `demo-password-three` |

Use `POST /auth/login` with any of these accounts. Passkey registration is optional on first login; an account that already has a passkey registered still requires it as MFA.

The demo users are persisted in the local PostgreSQL volume. If a previous passkey journey registered a passkey for an account and that passkey is unavailable in the browser, password login will intentionally show the MFA prompt. Start a clean reviewer stack with `docker compose -f deployment/compose/docker-compose.yml -f deployment/compose/docker-compose.jvm.yml -f deployment/compose/compose.fullstack.yml down --volumes` before bringing it up again, or use a seeded account without a registered passkey.

Redis is shared ephemeral infrastructure for horizontally scaled or serverless instances. It stores the ten-minute quote cache and five-minute WebAuthn ceremonies so a request can be completed by a different instance. PostgreSQL remains the source of truth for users, passkeys, refresh-token rotation, quotes, and business events; Redis is not used for durable state or distributed locks. Quote-cache failures fall back to PostgreSQL, while WebAuthn failures require restarting the ceremony. Redis is available at `localhost:6379` in the local Compose stack.

The observability overlay keeps ownership explicit: Actuator/Micrometer instruments the API, Prometheus scrapes `/actuator/prometheus`, and Grafana visualizes the resulting time series. Spring Boot exports request traces to Tempo through OTLP/HTTP, while Grafana Alloy reads Docker JSON logs and sends them to Loki. Grafana provisions all three sources with trace-to-log navigation. Kafka remains the durable transport for `QuoteSubmitted` domain events; it is not used as a metrics pipeline.

The API also uses Redis as a distributed fixed-window rate limiter for authentication and quote mutations. The Lua script atomically increments a bucket and applies its expiry, so multiple API instances share one decision. Redis failures fail open for availability and emit `rate_limit_redis_failures_total`; rejected requests return `429` with `Retry-After` and `X-RateLimit-*` headers.

Local telemetry ports are Prometheus `9090`, Grafana `3001` (`admin`/`admin`), Loki `3101`, and Tempo `3200` (query), `4317` (OTLP/gRPC), and `4318` (OTLP/HTTP). Loki and Tempo use short-lived filesystem storage through named Compose volumes. Alloy requires read-only access to the Docker socket for local collection; production deployments should replace that collector arrangement with a platform-native log pipeline.

Without mise, use the compose files directly:

```bash
docker-compose \
  -f deployment/compose/docker-compose.yml \
  -f deployment/compose/docker-compose.jvm.yml \
  -f deployment/compose/compose.fullstack.yml up -d --build
```

The repository’s `mise run up` task uses `docker compose` when available and falls back to the legacy `docker-compose` binary.

## GitHub Actions

Backend CI and the full-stack JVM smoke workflow run on every push and pull request. Runs are grouped by source branch and cancel older in-progress runs when a newer commit arrives. The backend workflow verifies Java 17, Testcontainers, the JVM API image, Compose configuration, and the sibling frontend baseline. Native-image compilation remains an explicit/manual path because of its documented memory requirements.

## Tests

```bash
mvn test                  # unit tests and docker-free slices
mvn verify                # integration tests, formatting, and JaCoCo gate
mvn verify -Pe2e          # backend black-box tests against a running stack
open service/target/site/jacoco/index.html
```

The domain/application coverage gate is 80%. The frontend repository owns the Playwright journeys; run them from the sibling repository after `mise run up jvm full e2e`:

```bash
cd ../insurance-quotes-web
E2E_BASE_URL=http://localhost:3100 bun run e2e
```

## How I approached it

1. I read the fixed contract and business rules first, then froze the API, pricing formula, quote states, and error semantics as non-decisions.
2. I designed the package boundaries and communication styles before implementation. The risky paths were the aggregate state machine, remote insurer call, expiration, money, authentication, and event publication.
3. I implemented the core with red-green-refactor tests, then added persistence, authentication, observability, API versioning, native packaging, and full-stack integration in separate increments.

## Design decisions

The ten accepted decisions are recorded in [`docs/decisions`](docs/decisions). The generated Modulith diagrams and module descriptions are in [`docs/architecture/modules`](docs/architecture/modules).

| Pattern | Example |
| --- | --- |
| DDD aggregate/state machine | `quote.domain.model.Quote` and `QuoteStatus` |
| Ports and adapters | `InsurerGateway`, repositories, and inbound web controllers |
| Strategy | Premium factors under `pricing.domain` |
| Outbox/event publication | Spring Modulith event registry to Kafka |
| Facade/use-case orchestration | Quote and submission application services |

The domain and application layers do not depend on HTTP status abstractions. Transport mapping stays in inbound adapters, and controllers use Spring’s native version-aware mappings.

## AI usage

This repository was built with AI pair-programming: design brainstorming, written specifications, phased plans, TDD implementation, and review checkpoints. Every generated change was inspected, tested, and committed by the developer. The package boundaries, domain/transport separation, eventing choice, and authentication scope were human-reviewed decisions; the trade-offs are recorded in the ADRs.

## Challenges / unfinished

- WebAuthn and stateless JWT required a Yubico integration because the ceremony state and refresh-token lifecycle need explicit application control.
- Native compilation is an optional profile and is intentionally slower than the JVM path. Spring Boot 4’s Paketo native builder requires Java 25 as build tooling, while the application and default runtime remain Java 17.
- The local environment used the legacy `docker-compose` executable because the Docker Compose plugin was unavailable; the mise task supports both forms.

## JVM vs native

The JVM image is the default reviewer path. The native image build reached GraalVM executable generation with optimization level 2, but the local Colima builder terminated it with exit status 137 after exhausting its memory. No native runtime number is fabricated; run the native build with a larger Docker/Colima memory allocation before comparing runtime behavior.

| Runtime | Startup | Memory | Result |
| --- | ---: | ---: | --- |
| JVM | 9.823 s | 438.3 MiB | Verified with the Java 17 Compose API container; image size 207,720,155 bytes |
| Native | Not measured | Not measured | Build stopped by local builder OOM (exit 137); Java 25 Paketo builder is build-only, application baseline remains Java 17 |

## Running both repositories

Expected sibling layout:

```text
workspace/
├── insurance-quotes-service/
└── insurance-quotes-web/
```

From this repository, one command starts the API and frontend:

```bash
mise run up jvm full
```

Then use `cd ../insurance-quotes-web && E2E_BASE_URL=http://localhost:3100 bun run e2e` for the browser journeys. The web app detects `navigator.languages`/`navigator.language`, normalizes to `en-US` or `es-MX`, and sends that locale as `Accept-Language` on API requests; unsupported browser locales fall back to `en-US`.
