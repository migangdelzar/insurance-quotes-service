# ADR-006: Submission orchestration outside database transactions

## Status

Accepted

## Date

2026-07-28

## Decision

Load and validate the quote, call the remote insurer without holding a database transaction, and then persist \`SUBMITTED\` or \`SUBMISSION_FAILED\` through a focused state update. A failed submission remains resubmittable and a successful submission is idempotent.

## Context and decision drivers

The insurer call is remote, slow, and failure-prone. Holding a database connection across network latency would reduce throughput and make a crashed remote call harder to recover.

## Considered alternatives

- **One transaction around the insurer call:** rejected because it holds a database resource during unbounded remote latency.
- **Mark submitted before the insurer call:** rejected because it can expose a false success when the remote call fails.
- **Asynchronous workflow as the default:** deferred because the challenge requires a synchronous user-facing result; the event boundary remains available for later asynchronous consumers.

## Implementation evidence

- \`submission/application/service/SubmitQuoteService.java\`
- \`submission/application/service/FinalizeQuoteSubmissionService.java\`
- \`submission/adapter/out/client/insurer/\`
- \`service/src/test/java/com/clara/insurancequotes/submission/application/service/\`
- \`service/src/integrationTest/java/com/clara/insurancequotes/submission/\`

## Consequences

### Positive

- Database connections are not held across insurer latency.
- Users receive a retryable failure instead of a stuck quote.
- A duplicate request cannot turn an already submitted quote into a second remote submission.

### Negative and operational

- The quote is observed across multiple short transactions.
- Insurer timeout, idempotency, and retry behavior need explicit monitoring.
- The external call is not atomically committed with the local state; the state machine and retry policy are the reconciliation mechanism.

## Related decisions

- [ADR-004: Quote state machine](ADR-004-data-driven-quote-state-machine.md)
- [ADR-007: Outbox events](ADR-007-outbox-kafka-events.md)
