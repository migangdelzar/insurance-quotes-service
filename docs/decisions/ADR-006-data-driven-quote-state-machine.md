# ADR-006: Data-driven quote state machine
Date: 2026-07-22 · Status: Accepted

## Context
Quote transitions have business invariants and must be easy to extend without scattering status switches across the aggregate.

## Decision
Keep transition capabilities and allowed operations on `QuoteStatus`; enforce transitions inside `Quote` aggregate methods.

## Consequences
The state machine is explicit, testable, and avoids enum-switch duplication. A switch-based transition table was rejected because it spreads policy and would rely on preview pattern-matching features that are not appropriate for Java 17.
