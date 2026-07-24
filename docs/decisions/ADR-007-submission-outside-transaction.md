# ADR-007: Submission orchestration outside database transactions
Date: 2026-07-22 · Status: Accepted

## Context
The insurer call is remote and can be slow or fail, while quote state must remain resubmittable after a failure.

## Decision
Load and validate the quote, call the insurer without holding a database transaction, then persist `SUBMITTED` or `SUBMISSION_FAILED` through a focused state update.

## Consequences
Database connections are not held across network latency and failed quotes can be retried idempotently. A single transactional method was rejected because a slow or crashed insurer call could hold resources and leave the aggregate stuck.
