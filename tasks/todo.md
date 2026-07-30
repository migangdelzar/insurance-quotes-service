# Modulith package structure refactor

## Acceptance criteria

- [ ] Every module follows the documented `api` / `application` / `domain` / `adapter` / `configuration` responsibilities.
- [ ] Class names consistently communicate commands, queries, results, use cases, services, events, exceptions, ports, and adapters.
- [ ] HTTP, persistence, Kafka, Redis, pricing, authentication, and submission behavior remains unchanged.
- [ ] Spring Modulith and architecture tests pass.
- [ ] The PR is pushed and ready for review.

## Progress

- [x] Design approved and committed (`ddee007`).
- [x] Implementation plan created at `docs/architecture/module-package-structure-plan.md`.
- [ ] Task 1 — module metadata and package vocabulary.
- [ ] Task 2 — shared configuration and observability.
- [ ] Task 3 — pricing naming.
- [ ] Task 4 — quote API split and public result naming.
- [ ] Task 5 — authentication transport boundary.
- [ ] Task 6 — submission naming.
- [ ] Task 7 — persistence/client/mapper naming.
- [ ] Task 8 — architecture enforcement and generated docs.
- [ ] Task 9 — full verification and PR handoff.

## Working notes

- Branch: `feat-modulith-package-structure`.
- Base: `origin/main`.
- The refactor is behavior-neutral; endpoint and JSON contracts must not change.
- Do not create empty responsibility packages only to match the diagram.

## Results

Implementation not started.
