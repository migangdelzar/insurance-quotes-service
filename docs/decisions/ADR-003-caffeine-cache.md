# ADR-003: Caffeine over Redis
Date: 2026-07-22 · Status: Accepted

## Context
The challenge service needs quote caching but runs as a single application instance in the evaluated deployment.

## Decision
Use Caffeine behind Spring’s cache abstraction for local quote caching and explicit eviction on state-changing operations.

## Consequences
The cache is fast and operationally small, but it is not shared across replicas. Redis was rejected because its operational weight is not justified until the service requires distributed cache coherence.
