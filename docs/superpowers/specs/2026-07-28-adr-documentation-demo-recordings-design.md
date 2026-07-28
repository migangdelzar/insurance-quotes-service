# ADR catalogue, documentation, and demo recordings design

| Field | Detail |
|---|---|
| Date | 2026-07-28 |
| Scope | Backend ADR catalogue, both repository READMEs/PRs, Playwright demo recordings |
| Status | Approved design checkpoint |
| Repositories | `insurance-quotes-service`, `insurance-quotes-web` |

## 1. Goal

Make the architectural record match the application that exists today, make the
two repositories understandable without reading their commit history, and
provide repeatable visual recordings of the real end-to-end journeys.

The work is documentation and verification focused. It does not change runtime
business behavior, API contracts, authentication semantics, or deployment
defaults.

## 2. ADR applicability audit

| Current ADR | Finding | Action |
|---|---|---|
| ADR-001 Modulith package boundaries | Active and foundational | Rewrite and move first in the active catalogue |
| ADR-002 Outbox/Kafka events | Active; implementation uses Spring Modulith externalization | Rewrite with the durable-event boundary and delivery consequences |
| ADR-003 Caffeine cache | No longer applicable; quote caching and WebAuthn ceremony state use Redis | Move to `docs/decisions/archive/` as superseded historical context |
| ADR-004 Flyway migrations | Active | Rewrite with the current PostgreSQL/Flyway runtime contract |
| ADR-005 JWT/WebAuthn | Active | Rewrite with password, refresh rotation, passkey enrollment, MFA, and passwordless flows |
| ADR-006 Quote state machine | Active | Rewrite with aggregate invariants and Java 17 constraints |
| ADR-007 Submission outside transactions | Active | Rewrite with retry/idempotency and insurer failure behavior |
| ADR-008 Observability tier | Active, but stale; Loki, Tempo, Alloy, OpenTelemetry, and Grafana now exist | Rewrite to separate metrics, logs, traces, and dashboards |
| ADR-009 Optional native runtime | Active | Rewrite with Java 17 runtime and Java 25 build-only native tooling |
| ADR-010 Extracted libraries | Active but lower impact | Rewrite as a deliberate reactor/library boundary |
| ADR-011 Redis shared ephemeral state | Active | Rewrite as quote cache and WebAuthn ceremony state; explicitly exclude durable state and locks |
| ADR-013 Redis rate limiting | Active | Renumber and rewrite as the shared HTTP protection boundary |

The old Caffeine decision is not silently deleted: its archived record states
that it was superseded by the Redis decision. The active catalogue uses a new
linear order rather than reusing the old number for a different decision.

## 3. Active ADR order

The new order is based on impact and dependency flow:

1. **Spring Modulith package boundaries** — the system shape and dependency rules.
2. **JWT refresh rotation and WebAuthn** — the security boundary used by every protected flow.
3. **PostgreSQL and Flyway migrations** — durable state and schema evolution.
4. **Data-driven quote state machine** — the central domain invariant.
5. **Redis shared ephemeral state** — cross-instance cache and ceremony coordination.
6. **Submission orchestration outside transactions** — remote side-effect boundary.
7. **Outbox publication to Kafka** — durable business-event integration boundary.
8. **Redis shared HTTP rate limiting** — cross-instance abuse protection.
9. **Actuator/Micrometer plus OpenTelemetry observability** — operational feedback paths.
10. **Optional native runtime** — packaging and performance alternative, not the default.
11. **Early extracted libraries** — reusable build boundaries with the lowest runtime impact.

The active files will be numbered `ADR-001` through `ADR-011`. The archived
Caffeine record will retain a historical filename and will not appear as an
active decision.

## 4. Consistent ADR format

Every active record will contain:

- status and date;
- one-sentence decision statement;
- context and decision drivers;
- considered alternatives with rejection reasons;
- implementation evidence (code/configuration locations);
- positive, negative, and operational consequences;
- related decisions and explicit supersession links;
- references to official framework documentation where useful.

`docs/decisions/README.md` will be the navigable index. It will include the
active order, lifecycle definitions, the old-to-new mapping, and the dependency
flow. A Mermaid graph will show the main relationships without implying that
Kafka owns metrics or that Redis owns durable business state.

## 5. README and pull-request documentation

Both repositories will receive a consistent documentation structure:

1. value proposition and challenge scope;
2. architecture-at-a-glance diagram;
3. repository boundaries and sibling checkout layout;
4. quick start and seeded development credentials;
5. API, browser, observability, and native/JVM commands;
6. verification matrix with current evidence;
7. CI/CD behavior and cancellation policy;
8. demo gallery and recording commands;
9. ADR index and troubleshooting;
10. review checklist and explicit non-goals.

The two PR descriptions will use the same sections, but each will clearly
identify which repository owns which concern. Historical failed or cancelled
workflow runs will not be presented as current evidence; links will point to
the latest successful head runs.

## 6. Playwright demo recordings

Add a dedicated `demo-recordings.spec.ts` suite that runs against the real
same-origin web/API stack and uses the existing virtual WebAuthn authenticator
for passkey flows. Each test has a stable, human-readable title and
`test.use({ video: 'on' })`.

The recordings cover:

| Recording | Journey |
|---|---|
| `01-standard-quote` | password login, standard quote, premium, successful submission |
| `02-senior-health-quote` | senior health questions including diabetes and hypertension, premium, submission |
| `03-submission-retry` | deterministic insurer failure, visible recovery, retry success |
| `04-passkey-lifecycle` | passkey setup, MFA assertion, and passwordless login |
| `05-history-and-analytics` | Home latest-four dashboard, full history, filtering, ordering, pagination |
| `06-observability` | real API request followed by health/metrics and dashboard-facing endpoints |

The browser test remains the source of truth. A manual GitHub Actions workflow
will run the suite against an ephemeral JVM Compose stack, retain MP4/WebM
Playwright videos and screenshots, and upload a named artifact. Local runs use
the same command and write recordings under the ignored Playwright output
directory; no large binary files are committed to git.

`docs/demo/flow-hyperframes.md` will provide the visual gallery in Markdown:
each flow has a three-frame “setup → action → outcome” storyboard, the exact
Playwright test link, expected API boundary, and the corresponding Actions
artifact link pattern. This keeps the visual narrative reviewable while the
binary recordings remain downloadable artifacts.

## 7. Verification and rollback

- Validate every Markdown file and all relative ADR links.
- Run frontend formatting, lint, typecheck/build, and the focused demo suite.
- Run the real full-stack Playwright recording suite with retries disabled.
- Run backend Maven verification with the existing integration-test environment.
- Validate both Compose configurations and the Actions YAML.
- Inspect git status in both repositories and preserve unrelated user edits.

The changes are documentation/test/workflow-only and can be rolled back by
reverting the documentation and recording commits. The archived Caffeine ADR is
recoverable independently from the active catalogue.

