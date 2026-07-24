# insurance-quotes-service

Spring Boot 4.0.6 / Java 17 backend for the insurance quote flow. It is a DDD and hexagonal Spring Modulith: business capabilities are package modules, domain code is transport-neutral, and inbound/outbound adapters are explicit.

Sibling frontend: [insurance-quotes-web](../insurance-quotes-web)

## Run

Install the pinned tools and hooks, then start the default JVM stack:

```bash
mise run setup
mise run up                         # PostgreSQL + Kafka + JVM API
mise run up jvm observability       # add Prometheus :9090 and Grafana :3001
mise run up jvm full                # add the frontend at http://localhost:3000
mise run up jvm full e2e            # add WireMock at http://localhost:8089
```

The full-stack command expects the sibling frontend directory shown above. The API is available at `http://localhost:8080`; Swagger UI is at `/swagger-ui.html`. Demo credentials are `demo` / `demo-password` for `POST /auth/login`.

Without mise, use the compose files directly:

```bash
docker-compose \
  -f deployment/compose/docker-compose.yml \
  -f deployment/compose/docker-compose.jvm.yml \
  -f deployment/compose/compose.fullstack.yml up -d --build
```

The repository’s `mise run up` task uses `docker compose` when available and falls back to the legacy `docker-compose` binary.

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
E2E_BASE_URL=http://localhost:3000 bun run e2e
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
- Native compilation is an optional profile and is intentionally slower than the JVM path; the final measured comparison is recorded below when the native verification is available in the current environment.
- The local environment used the legacy `docker-compose` executable because the Docker Compose plugin was unavailable; the mise task supports both forms.

## JVM vs native

The JVM image is the default reviewer path. Native-image availability and measured startup/RSS results are environment-dependent; they must be recorded from an actual `mise run native`/Compose run rather than estimated. Current verification status is tracked by the final Plan 4 checks.

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

Then use `cd ../insurance-quotes-web && bun run e2e` for the browser journeys.
