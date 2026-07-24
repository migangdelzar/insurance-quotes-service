# GitHub Actions CI Plan

| Field | Detail |
|---|---|
| **Design specification** | [2026-07-24-github-actions-ci-design.md](../specs/2026-07-24-github-actions-ci-design.md) |
| **Date** | 2026-07-24 |
| **Status** | Complete |

## Tasks

| # | Task | Status |
|---|---|---|
| 1 | Add backend Java/Testcontainers/Docker/Compose CI workflow with branch-run cancellation. | ✅ Done |
| 2 | Add backend full-stack JVM Compose smoke workflow with cleanup. | ✅ Done |
| 3 | Add frontend Bun/test/lint/format/build/responsive-browser workflow with branch-run cancellation. | ✅ Done |
| 4 | Document workflow triggers, port `3100`, and native-image policy. | ✅ Done |
| 5 | Validate workflow YAML, local commands, Compose startup, and clean Git state. | ✅ Done |

## Definition of done

- [x] All workflows trigger on pushes and pull requests.
- [x] Concurrency cancellation is configured in every workflow.
- [x] Backend and frontend checks are represented in Actions.
- [x] Full-stack JVM smoke verification is represented in Actions.
- [x] Native-image files remain unchanged.
- [x] Workflows and documentation are committed and pushed.

## Verification evidence

- Workflow YAML passed `yamllint` and Ruby YAML parsing in both repositories.
- Backend `env TESTCONTAINERS_RYUK_DISABLED=true mise exec -- mvn -pl service verify` passed with 58 unit tests, 9 integration tests, Spotless, and JaCoCo coverage.
- Frontend unit tests (32), lint, targeted Prettier checks, and production build passed.
- The full Playwright journey set passed sequentially against the Docker stack on port `3100`: standard adult, senior health-data, insurer failure/retry, and passkey enrollment/authentication.
- Responsive Playwright coverage passed at 320, 375, 768, and 1280 pixels.
- The complete local Compose configuration was validated and the API, web, PostgreSQL, Kafka, Redis, and WireMock services reached healthy/running state.
- Observability was verified with the observability overlay: `/actuator/prometheus` returned metrics, the Prometheus API target was `up`, Grafana's Prometheus proxy returned `up=1`, and the provisioned six-panel `Insurance Quotes` dashboard loaded successfully.
- The full-stack workflow intentionally smoke-tests the backend branch with the frontend repository's `main`; feature-branch Playwright journeys were verified locally against both current worktrees.
