# ADR-011: Early extraction of shared reactor libraries

## Status

Accepted

## Date

2026-07-28

## Decision

Keep \`throwing-functions\` and \`service-i18n\` as independently named Maven reactor libraries even though this challenge is their first compiled consumer. The service consumes them through explicit library dependencies rather than copying their utilities into a business module.

## Context and decision drivers

The organization’s modulith template expects reusable error-handling functions and localization conventions to be independently versionable. Preserving the boundary also makes future services and shared tooling consumers possible.

## Considered alternatives

- **Copy utilities into \`service\`:** rejected because it would hide the reuse boundary and create divergent implementations.
- **Publish immediately to a remote registry:** rejected because the current challenge needs a reproducible local reactor and does not yet have a release registry contract.
- **Wait for a second consumer:** rejected because the template boundary is an intentional architecture constraint, not an accidental abstraction.

## Implementation evidence

- \`libraries/throwing-functions/\`
- \`libraries/service-i18n/\`
- root \`pom.xml\` reactor modules
- \`service/pom.xml\` dependencies
- \`docs/superpowers/specs/2026-07-22-package-structure-design.md\`

## Consequences

### Positive

- Shared behavior has an explicit ownership and versioning boundary.
- The service remains focused on insurance business capabilities.
- Future consumers can adopt the libraries without moving service packages.

### Negative and operational

- The Maven reactor has more modules and dependency ordering.
- Library APIs require compatibility discipline before external publication.
- The second-consumer assumption is a planned organizational use case rather than current production evidence.

## Related decisions

- [ADR-001: Module boundaries](ADR-001-spring-modulith-package-boundaries.md)
