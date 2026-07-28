# Historical ADR-003: Caffeine over Redis

## Status

Superseded by [ADR-005: Redis for shared ephemeral state](../ADR-005-redis-shared-ephemeral-state.md)

## Historical decision

The original challenge design selected Caffeine behind Spring’s cache abstraction for a single-instance deployment.

## Why it is no longer applicable

The implemented service now supports horizontally scaled or serverless instances, and both quote caching and WebAuthn ceremony state use Redis. The current dependency graph contains Spring’s cache abstraction and Redis support, not the Caffeine implementation.

This record is retained only to explain the evolution of the architecture. It is not an active deployment recommendation.
