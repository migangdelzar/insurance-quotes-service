# ADR-005: JWT access tokens with refresh rotation and WebAuthn
Date: 2026-07-22 · Status: Accepted

## Context
The API needs stateless request authentication, revocable sessions, and a passkey path that works as MFA and passwordless login.

## Decision
Use short-lived JWT access tokens, rotating refresh-token families with reuse detection, and Yubico WebAuthn ceremonies backed by a short-lived cache.

## Consequences
The design provides scalable request validation and meaningful session revocation while accepting ceremony and credential complexity. API keys were rejected for the required user-session semantics; an access-token denylist was rejected because short TTLs provide a bounded residual risk with less state.
