# ADR-004: Data-driven quote state machine

## Status

Accepted

## Date

2026-07-28

## Decision

Keep allowed quote transitions and capabilities on \`QuoteStatus\`, and enforce them inside the \`Quote\` aggregate methods. Application services may orchestrate the use case, but they may not bypass aggregate invariants by assigning status directly.

## Context and decision drivers

Quotes move through draft, submitted, and failed-submission states. Submission must reject incomplete data, failed submissions must remain retryable, and new states should not require status checks scattered across services.

## Considered alternatives

- **Switch statements in application services:** rejected because policy would be duplicated and drift between use cases.
- **An external workflow engine:** rejected because the current state graph is small, synchronous, and part of the domain model.
- **Java preview pattern matching:** rejected because the runtime compatibility floor is Java 17.

## Implementation evidence

- \`quote/domain/model/Quote.java\`
- \`quote/domain/model/QuoteStatus.java\`
- \`quote/domain/exception/\`
- \`service/src/test/java/com/clara/insurancequotes/quote/domain/model/QuoteTest.java\`

## Consequences

### Positive

- Invariants have one owner and are directly unit-tested.
- Invalid transitions fail before persistence or remote calls.
- Failed operations preserve the previous state because mutation occurs only after validation and successful domain decisions.

### Negative and operational

- The aggregate must be changed deliberately when a new business state is introduced.
- API error mapping remains an adapter concern and must translate domain exceptions without coupling the domain to HTTP.

## Related decisions

- [ADR-001: Module boundaries](ADR-001-spring-modulith-package-boundaries.md)
- [ADR-006: Submission boundary](ADR-006-submission-outside-transaction.md)
