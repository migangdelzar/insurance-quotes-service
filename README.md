# Clara Insurance Quotes · API

![Java 17](https://img.shields.io/badge/runtime-Java%2017-007396?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-6DB33F?style=flat-square)
![Architecture](https://img.shields.io/badge/architecture-DDD%20%2B%20hexagonal%20%2B%20Modulith-111827?style=flat-square)
![CI](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF?style=flat-square)

The Clara backend is a Java 17 Spring Boot service for the insurance quote
challenge. It is a DDD and hexagonal Spring Modulith: quote, authentication,
pricing, and submission capabilities share one deployable runtime while
keeping domain rules independent from HTTP, PostgreSQL, Redis, Kafka, and
observability adapters.

> **Start here:** the sibling frontend is
> [insurance-quotes-web](../insurance-quotes-web). The integrated application
> uses same-origin browser traffic through the frontend proxy.

## Architecture at a glance

~~~mermaid
flowchart LR
  browser[React/Vite browser] --> web[Nginx or Vite /api proxy]
  web --> api[Spring Boot API]
  api --> quote[Quote domain]
  api --> auth[Auth and WebAuthn]
  api --> pricing[Pricing]
  api --> submission[Submission]
  quote --> postgres[(PostgreSQL)]
  auth --> postgres
  api --> redis[(Redis)]
  submission --> insurer[InsurerGateway / WireMock]
  submission --> outbox[Spring Modulith outbox]
  outbox --> kafka[(Kafka)]
  api --> metrics[Actuator + Micrometer]
  metrics --> prometheus[(Prometheus)]
  prometheus --> grafana[Grafana]
  api --> traces[OpenTelemetry]
  traces --> tempo[(Tempo)]
  api --> logs[JSON stdout]
  logs --> alloy[Alloy]
  alloy --> loki[(Loki)]
~~~

## Responsibilities and boundaries

| Concern | Runtime owner | Boundary |
|---|---|---|
| Durable users, passkeys, refresh state, quotes, and migrations | PostgreSQL + Flyway | Source of truth |
| Quote cache and WebAuthn ceremonies | Redis | Bounded ephemeral state |
| Quote business events | Spring Modulith outbox → Kafka | Durable event delivery |
| Operational metrics | Actuator/Micrometer → Prometheus | Scrape/query path |
| Logs | JSON stdout → Alloy → Loki | Correlated log search |
| Traces | OpenTelemetry → Tempo | Distributed request diagnosis |
| HTTP protection | Redis fixed-window Lua buckets | Shared rate-limit decisions |
| Application runtime | Java 17 JVM | Default reviewer/deployment path |

Kafka is not the metrics pipeline. Redis is not durable business state and is
not used as a general-purpose distributed lock service.

## Quick start

> **Full-stack runbook:** for the complete one-command setup, HMR workflow,
> real Playwright verification, observability checks, and troubleshooting, see
> the [Full-stack setup and verification guide](../insurance-quotes-web/docs/setup-and-verification.md).

### Prerequisites

The supported demo baseline is intentionally small and local:

| Requirement | Minimum / recommended value |
|---|---|
| Java | 17 (runtime and build) |
| Maven | 3.9+ (or the pinned `mise` tool) |
| Docker | Docker Engine with Compose v2, or legacy `docker-compose` |
| Container runtime | Colima with 2 CPUs and 4 GiB RAM, or an equivalent Docker VM |
| Disk | At least 8 GiB free for images and the demo volumes |
| Frontend runtime | Bun, for the sibling web workspace |
| Ports | `3100` web, `8080` API, `5432` PostgreSQL, `6379` Redis, `8089` WireMock, `9094` Kafka |

The JVM demo API is capped at 512 MiB and the supporting services are kept
small enough for the 4 GiB Colima profile. Native images use a lower memory
cap, but are optional and are not required to run the demo.
GitHub smoke tests use a CI-only 768 MiB API cap to absorb cold-start class
loading without changing the local demo budget.

- Java 17
- Maven 3.9+
- Docker and Compose
- mise (recommended)
- sibling checkout of the frontend for full-stack flows

For Colima, the recommended low-memory setup is:

~~~bash
colima stop --force
colima start --cpu 2 --memory 4
~~~

Use `colima status` and `docker info` to confirm the active runtime before
starting the stack.

Expected layout:

~~~text
workspace/
├── insurance-quotes-service/
└── insurance-quotes-web/
~~~

### JVM stack

~~~bash
mise run setup
mise run up                         # PostgreSQL + Kafka + Redis + API
mise run up jvm full e2e            # add Nginx :3100 and WireMock :8089
~~~

The API is available at http://localhost:8080. Swagger UI is at
http://localhost:8080/swagger-ui.html.

### One-command reviewer demo

For the easiest full-stack setup, keep the sibling repositories in the layout
shown above and run this from `insurance-quotes-service`:

~~~bash
mise run demo
~~~

This installs both workspaces, builds and starts the JVM Compose stack, and
prints the browser URL and seeded credentials. Open http://localhost:3100 and
sign in with `demo` / `demo-password`. Stop it with:

~~~bash
mise run stop
~~~

The command is idempotent: if a healthy Clara stack is already serving
`localhost:3100`, it reuses that stack instead of rebuilding or replacing it.

If the frontend checkout is elsewhere, set `CLARA_WEB_DIR` before running the
demo.

To return the demo to a clean state after passkey or quote journeys, run the
explicit reset command:

~~~bash
mise run reset-demo
~~~

It removes the local PostgreSQL volume and recreates the Redis and Kafka demo
containers before restarting the JVM full-stack stack. The command asks you to
type `reset`; for a non-interactive local script, use:

~~~bash
DEMO_RESET_CONFIRM=reset mise run reset-demo
~~~

The command rejects the `prod` mise environment or Spring profile and never
runs automatically at application startup. It is intended only for local demo
data.

### Demo verification checklist

After setup, these checks exercise the real browser proxy and API:

~~~bash
curl -fsS http://localhost:3100/api/actuator/health
curl -fsS -X POST http://localhost:3100/api/auth/login \
  -H 'content-type: application/json' \
  --data '{"username":"demo","password":"demo-password"}'
~~~

The login response contains short-lived tokens; do not paste them into issue
reports or documentation. The Playwright suite in the sibling web workspace
should be run against `http://localhost:3100` so it verifies same-origin BFF
routing rather than calling the API directly.

### Fast hot-reload loop

~~~bash
mise run dev-infra                  # PostgreSQL, Kafka, Redis, WireMock
mise run dev                        # Spring DevTools + LiveReload :35729
cd ../insurance-quotes-web
bun run dev:hmr                     # Vite HMR :5173
~~~

The dev profile disables rate limiting for fast local iteration and is not the
production security posture. Docker and production profiles retain Redis
rate limiting, caching, and packaging defaults.

## Development users and passkeys

The local profile seeds these accounts:

| Username | Password | Purpose |
|---|---|---|
| demo | demo-password | Standard, senior, retry, and history journeys |
| demo-two | demo-password-two | Isolated passkey lifecycle journey |
| demo-three | demo-password-three | Independent manual session |
| demo-admin | demo-admin-password | Read-only oversight across every user's quotes |

The first password login offers passkey enrollment. Passwordless login for an
account without a credential returns an actionable passkey-not-registered
error. If a previous browser journey registered a passkey, reset the local demo
before using password-only recovery:

~~~bash
mise run reset-demo
~~~

## API and business flow

The controller layer exposes versioned HTTP contracts. The domain remains
transport-neutral and owns quote transitions, completeness, senior health
rules, diabetes/hypertension pricing factors, and retryable submission state.

~~~text
login → create draft → personal details → coverage and health
     → premium calculation → submit to insurer → submitted or retryable failure
     → paginated history and analytics
~~~

The browser calls the frontend origin at /api. Nginx/Vite proxies that path to
the API, so the deployed browser flow does not call the backend or insurer
directly and does not require browser CORS.

## Observability

Start the local observability overlay:

~~~bash
mise run up jvm observability
~~~

| Tool | URL | Role |
|---|---|---|
| Actuator | http://localhost:8080/actuator | Health, metrics, Prometheus, Modulith |
| Prometheus | http://localhost:9090 | Scraped time series |
| Grafana | http://localhost:3001 | Dashboards; admin/admin locally |
| Loki | http://localhost:3101 | Structured logs |
| Tempo | http://localhost:3200 | Trace query |
| Tempo OTLP | localhost:4317 / 4318 | gRPC / HTTP trace ingestion |

Business meters cover quote lifecycle, submission latency and outcomes, cache
failures, and rate-limit behavior. The dashboard is provisioned from
deployment/compose/observability/grafana/dashboards/quotes.json.

## Verification

~~~bash
mvn -B test
mvn -B verify
mvn -B verify -Pe2e
~~~

The service JaCoCo gate is 80%. For the real browser journeys:

~~~bash
cd ../insurance-quotes-web
E2E_BASE_URL=http://localhost:3100 bun run e2e -- --retries=0
~~~

The browser recording workflow is documented in
[docs/demo-recordings.md](../insurance-quotes-web/docs/demo-recordings.md).

## JVM versus native

Java 17 is the application runtime. Native compilation is an explicit
comparison path; Java 25 is build-only tooling for the Spring Boot 4/Paketo
native builder.

~~~bash
mise run native
RUNTIME_REPORT_PATH=/tmp/clara-runtime-comparison.md ./scripts/compare-runtimes.sh
cat /tmp/clara-runtime-comparison.md
~~~

The comparison measures startup, health latency, elapsed time, RSS, and image
size for the same API. The local comparison command produces a report without
adding a required CI check.

## CI/CD

- [Backend CI](.github/workflows/ci.yml) runs Java 17 verification, reactor
  dependency resolution, image build, and Compose configuration checks.
- [Full-stack JVM smoke](.github/workflows/full-stack-smoke.yml) verifies the
  API, Nginx frontend, seeded login, and analytics against compatible refs.
- Native comparison is local/manual
  because native compilation requires more memory and is not required in CI.
- Push and pull-request runs use concurrency cancellation so a newer commit
  stops an older in-progress run for the same branch.

The workflows validate deployable images and full-stack behavior. Cloud
deployment is intentionally target-neutral until a registry, hosting target,
and credentials are selected.

## Architecture decisions

The maintained catalogue is in [docs/decisions](docs/decisions/README.md).
The [decision flow](docs/decisions/decision-flow.md) explains the separation
between durable state, Redis state, Kafka business events, metrics, logs, and
traces.

The most important decisions are:

1. DDD, hexagonal, responsibility-based Spring Modulith boundaries.
2. JWT refresh rotation and WebAuthn for session/passkey flows.
3. PostgreSQL/Flyway for durable state.
4. Redis for bounded shared state and rate limiting.
5. Actuator/Micrometer/OpenTelemetry for operational visibility.

## Challenge map

| Requirement | Implementation |
|---|---|
| Quote lifecycle | Quote aggregate, state machine, application services |
| Senior health pricing | Health profile and pricing strategy |
| Authentication | Password, JWT refresh rotation, WebAuthn enrollment/MFA/passwordless |
| Reliable submission | Insurer port, retryable failure state, outbox publication |
| Scalable transient state | Redis cache, ceremonies, and rate limits |
| Localized API errors | service-i18n library and locale-aware error mapping |
| Operability | Actuator, Prometheus, Grafana, Loki, Tempo, Alloy |
| Packaging | Java 17 JVM default and optional native image |

## Troubleshooting

- **Port 3000 is occupied:** use the application’s Nginx port 3100.
- **Passkey asks for an unavailable credential:** reset the local volumes or
  use another seeded user.
- **Quote submission is slow:** local full-stack uses WireMock; verify the
  WireMock service is healthy on port 8089.
- **Redis is unavailable:** quote reads fail open to PostgreSQL; WebAuthn
  ceremonies must be restarted; rate limiting emits a warning and fails open.
- **Native build exits 137:** increase the Docker/Colima memory allocation and
  rerun the manual comparison path.

## Contributing

Keep commits focused and use conventional commit messages. New architecture
decisions belong in docs/decisions with an index update. New browser behavior
should have a real Playwright journey or a focused component contract.
