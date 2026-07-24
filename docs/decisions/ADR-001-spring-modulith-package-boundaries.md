# ADR-001: Spring Modulith at package level
Date: 2026-07-22 · Status: Accepted

## Context
The service contains several business capabilities that must remain independently understandable while sharing one deployable runtime.

## Decision
Use Spring Modulith package modules inside one Maven reactor and verify boundaries with `ApplicationModules` tests.

## Consequences
We keep one straightforward build and deployment unit while making dependencies executable and reviewable. Separate Maven modules per business capability were rejected because their build ceremony would not add reviewer value here.
