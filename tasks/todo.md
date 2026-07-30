# Modulith package structure refactor

## Acceptance criteria

- [ ] Every module follows the documented `api` / `application` / `domain` / `adapter` / `configuration` responsibilities.
- [ ] Class names consistently communicate commands, queries, results, use cases, services, events, exceptions, ports, and adapters.
- [x] HTTP, persistence, Kafka, Redis, pricing, authentication, and submission behavior remains unchanged.
- [x] Spring Modulith and architecture tests pass.
- [ ] The PR is pushed and ready for review.

## Progress

- [x] Design approved and committed (`ddee007`).
- [x] Implementation plan created at `docs/architecture/module-package-structure-plan.md`.
- [x] Task 1 — module metadata and package vocabulary (`dad1da4`, `39dba30`, `30b908f`).
- [x] Task 2 — shared configuration and observability (`e6d7e83`).
- [x] Task 3 — pricing naming (`dc3a172`, follow-up naming/docs fixes in this commit).
- [x] Task 4 — quote API split and public result naming (`4da421e`).
- [x] Task 5 — authentication transport boundary (`d5a7e80`).
- [x] Task 6 — submission naming (`1c32cbb`; formatting follow-up `d7edb33`).
- [x] Task 7 — persistence/client/mapper naming (`99766aa`).
- [x] Task 8 — architecture enforcement and generated docs (`acd47ed`).
- [ ] Task 9 — full verification and PR handoff.

## Working notes

- Branch: `feat-modulith-package-structure`.
- Base: `origin/main`.
- The refactor is behavior-neutral; endpoint and JSON contracts must not change.
- Do not create empty responsibility packages only to match the diagram.

## Results

Task 1 is complete. Focused Modulith verification passes with 4 tests and 0
failures. The generated PlantUML relationship ordering was refreshed after
package metadata generation and is committed with the Task 1 documentation.

Task 2 moves shared observability, web filtering, and configuration into their
responsibility packages. The legacy configuration bean names remain stable even
though the configuration class names are now consistent.

The Task 2 generated component diagram refresh is included in the next
documentation checkpoint commit.

Task 3 standardizes the Pricing module's command, use-case, and application
service names while preserving premium calculation behavior and its public
`Premium` result.

Task 4 replaces the broad Quote API with focused use-case contracts and matching
application services. Quote results use business names, and Submission consumes
only public Quote API models and use cases; it no longer depends on Quote domain
status types. Controller JSON fields, caching, metrics, pricing, ownership, and
administrator behavior are covered by focused regression tests.

Task 6 standardizes Submission capabilities and adapter names without changing
the HTTPBin insurer boundary. The real integration test requires a working
Docker/Testcontainers runtime; its local attempt reported the missing Docker
socket, while all focused submission and Modulith tests pass.
