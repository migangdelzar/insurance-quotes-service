# Lessons

## 2026-07-23 — package responsibility correction

- Failure mode: treating every exception as an API exception made the package structure imply that application and domain failures belonged to the transport layer.
- Detection signal: `QuoteNotFoundException` represented a use-case lookup failure, while `ApiException` represented HTTP concerns.
- Prevention rule: classify exceptions by responsibility first; keep domain/application exceptions framework-neutral and translate them only at inbound adapter boundaries.

