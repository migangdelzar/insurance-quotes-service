# ADR-002: JWT refresh rotation and WebAuthn authentication

## Status

Accepted

## Date

2026-07-28

## Decision

Use short-lived JWT access tokens, rotating refresh-token families with reuse detection, and Yubico WebAuthn ceremonies for passkey enrollment, MFA, and passwordless login. Ceremony challenges are stored through an outbound port in short-lived Redis state.

## Context and decision drivers

The browser needs password login for development and recovery, scalable request authentication, revocable sessions, and a passkey path that gives a useful experience when a credential is not registered or a challenge expires.

## Considered alternatives

- **API keys:** rejected because they do not model user sessions, rotation, or passkey ceremonies.
- **Long-lived access tokens:** rejected because the residual compromise window is too large for a browser session.
- **Access-token denylist:** rejected because short access-token TTLs provide bounded residual risk without a write on every request.
- **Instance-local ceremony state:** rejected because a subsequent request may reach a different JVM instance.

## Implementation evidence

- \`service/src/main/java/com/clara/insurancequotes/auth/\`
- \`auth/application/port/out/WebAuthnCeremonyStore.java\`
- \`auth/adapter/out/cache/RedisWebAuthnCeremonyStore.java\`
- \`service/src/main/resources/application.yml\`
- \`service/src/test/java/com/clara/insurancequotes/auth/\`

## Consequences

### Positive

- Access-token validation is stateless and horizontally scalable.
- Refresh rotation supports session revocation and reuse detection.
- Passkey setup, MFA, and passwordless login share a standards-based ceremony.

### Negative and operational

- WebAuthn adds browser capability, origin, credential, and ceremony failure cases that must be shown as actionable UI errors.
- Redis availability is required to continue an in-flight ceremony; the user must restart an expired or unavailable ceremony.
- JWT signing secrets and WebAuthn origins must be configured per environment.

## Related decisions

- [ADR-001: Module boundaries](ADR-001-spring-modulith-package-boundaries.md)
- [ADR-005: Redis ephemeral state](ADR-005-redis-shared-ephemeral-state.md)
