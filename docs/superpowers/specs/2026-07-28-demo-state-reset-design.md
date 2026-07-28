# Demo State Reset Design

**Date:** 2026-07-28  
**Status:** Implemented and verified
**Scope:** Local JVM full-stack demo only

## Goal

Provide one explicit command that returns the local Clara demo to a clean,
repeatable state and starts it again. This is useful when passkeys, users,
quotes, cache entries, rate-limit buckets, or Kafka delivery state from an
earlier browser journey make a demo difficult to repeat.

## Chosen approach

Add `mise run reset-demo`, backed by `scripts/reset-demo.sh`.

The command will:

1. Resolve either `docker compose` or the legacy `docker-compose` executable.
2. Require an explicit confirmation (`DEMO_RESET_CONFIRM=reset`) in
   non-interactive environments and ask for the same confirmation in an
   interactive terminal.
3. Refuse to run when a production profile is selected.
4. Stop the local JVM full-stack Compose project with `--volumes` and
   `--remove-orphans`.
5. Start the JVM full-stack demo again with the existing `mise run up jvm full
   e2e` workflow.

The reset scope is intentionally the Compose demo project only:

- PostgreSQL's named `postgres-data` volume is removed, so Flyway recreates
  the schema and demo seed logic starts from an empty database.
- Redis has no persistent volume in the demo Compose configuration, so
  stopping and recreating its container clears caches, WebAuthn ceremonies,
  and rate-limit buckets.
- Kafka has no persistent volume in the demo Compose configuration, so
  stopping and recreating its container clears broker state and demo topics.

The application will never reset data automatically during startup. The
existing `mise run stop` command remains a stop-and-remove operation without a
restart.

## Safety boundary

This command is for local demo data, not production operations. It will fail
unless the user explicitly confirms the destructive reset, and it will reject
profiles containing `prod`. The command does not execute arbitrary SQL or
connect to a database selected by an untrusted runtime argument.

## Alternatives considered

### Manual Compose commands

This already works but requires remembering the correct file list and can
leave the user with stopped services. It is too error-prone for repeated
browser demonstrations.

### Application reset endpoint

Rejected because a reset endpoint would add a destructive HTTP capability to
the deployed application and would require additional authorization and
auditing. Resetting infrastructure outside the application keeps the feature
out of the runtime attack surface.

### SQL-only reset script

Rejected as the primary mechanism because it would need to preserve ordering
between PostgreSQL tables and separately clear Redis and Kafka state. Compose
volume/container recreation gives the demo a deterministic clean boundary.

## Verification plan

Verification completed after the per-user quote ownership migration:

- `bash -n scripts/reset-demo.sh` passes;
- the command refuses without `DEMO_RESET_CONFIRM=reset`;
- the command refuses `MISE_ENV=prod` before Compose teardown;
- a confirmed reset removes the local PostgreSQL volume and recreates the
  JVM/full-stack/e2e Compose stack;
- the API and same-origin web proxy health endpoints are `UP`;
- a fresh real API login succeeds with the seeded demo account;
- the service unit suite passes, including indexed demo-user property binding;
- no unrelated Compose project is targeted by the reset file list.
