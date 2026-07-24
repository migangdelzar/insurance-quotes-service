# GitHub Actions CI Design

| Field | Detail |
|---|---|
| **Date** | 2026-07-24 |
| **Repositories** | `insurance-quotes-service`, `insurance-quotes-web` |
| **Status** | Approved for implementation |

## Goal

Provide reliable pull-request and push validation for both repositories while preserving the existing Java 17 JVM and optional native-image paths. A newer run for the same branch or pull request must cancel an older in-progress run.

## Workflow structure

### Backend repository

`ci.yml` runs on every `push` and `pull_request` event. It runs Java 17 Maven verification, Testcontainers integration tests, Docker API image compilation, and Compose configuration validation.

`full-stack-smoke.yml` runs the existing sibling-repository Compose stack with the current backend revision and the frontend `main` revision. It waits for API health, verifies the frontend on port `3100`, and always tears down containers and volumes.

### Frontend repository

`ci.yml` runs on every `push` and `pull_request` event. It installs the locked Bun dependencies, runs web tests, lint, targeted Prettier checks, production build, and the four-viewport Playwright responsive audit against a Vite server on port `3100`.

## Cancellation

Each workflow uses a concurrency group composed from the source repository and branch name. For pull requests, the source branch is used; for direct pushes, the pushed ref is used. `cancel-in-progress: true` ensures a newer push supersedes an older run even when both push and pull-request events are emitted.

## Native image policy

The native Docker image and `mise run native` path remain unchanged. Native compilation is not part of normal PR CI because the repository already documents its higher memory cost and Java 25 build-tool requirement. It remains available as an explicit local/manual operation.

## Security and cleanup

Workflows request only `contents: read`, use ephemeral GitHub-hosted runners, avoid secrets, and clean Compose resources in an `always()` step. Cross-repository checkout in the backend smoke workflow is read-only and uses the public frontend default branch.

## Acceptance criteria

- Every push and pull request starts the applicable workflow.
- A newer run cancels the older run for the same source branch.
- Backend and frontend quality gates are automated.
- Full-stack JVM Compose startup is smoke-tested.
- The insurance web port is consistently `3100`.
- Native-vs-JVM files and commands are not moved or removed.
