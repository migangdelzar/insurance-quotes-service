# ADR-010: Early extraction of shared libraries
Date: 2026-07-22 · Status: Accepted

## Context
The organization’s service template expects reusable error-handling functions and localization conventions to be independently versionable.

## Decision
Keep `throwing-functions` and `service-i18n` as reactor libraries even though this service is their first compiled consumer.

## Consequences
The structure establishes the intended reuse boundary and keeps the service modules focused, at the cost of extra reactor wiring today. The usual two-consumer rule is deliberately waived; the sibling frontend convention and future services are the planned second consumers.
