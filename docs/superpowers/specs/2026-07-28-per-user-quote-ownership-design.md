# Per-User Quote Ownership — Design

**Date:** 2026-07-28
**Status:** Approved, pending implementation
**Related:** `docs/use_cases.puml`, `docs/use_cases/UC-003..005-*.md`, `docs/entity_model.md`
(reverse-engineered before this change; will need updating once implemented)

## 1. Problem

The reverse-engineered use cases and entity model surfaced that `quotes` has no
`user_id` — any authenticated user can list, view, and see analytics over
every quote in the system, regardless of who created it. The original PDF
spec describes a public, self-service quote calculator (the signed-in user
*is* the applicant), which doesn't fit a shared, unscoped pool. This design
adds per-user ownership, plus a read-only admin role for oversight, on top of
the existing Spring Security JWT setup.

## 2. Decisions made during brainstorming

- **Ownership is strict for regular users**: every endpoint scoped to the
  requester's own quotes, no exceptions.
- **A privileged ADMIN role exists**, added specifically for oversight.
- **Admin is read-only across other users' quotes**: `getQuote`,
  `listQuotes`, `getSummary` are unscoped for an admin; `create`,
  `updateCoverage`, and the submit flow (`ensureSubmittable`,
  `markSubmitted`, `markSubmissionFailed`) remain owner-scoped for
  *everyone*, admins included. Admin never mutates another user's quote.
- **Cross-user access to a single quote returns 404**, not 403 — reuses the
  existing `QuoteNotFoundException` semantics rather than confirming a quote
  ID exists to someone who doesn't own it.
- **No backfill migration**: `quotes.user_id` is added `NOT NULL` with no
  default. There is no seed data in `quotes` today (confirmed: no
  `INSERT INTO quotes` in any Flyway migration), so this is safe for a
  fresh/dev database. This would break on a populated production table —
  acceptable here, called out explicitly in case that assumption is wrong.

## 3. Data model changes

### `users` (new migration, `V5__add_role_to_users.sql`)

```sql
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
```

- Values: `USER`, `ADMIN`.
- `User` domain model (`auth/domain/model/User.java`) gains a `role` field.
- `DemoUserProperties.User` gains an optional `role` (default `USER` when
  unspecified).
- A new seeded demo admin account is added (`demo-admin` /
  `demo-admin-password` by default, overridable via env vars following the
  existing `AUTH_DEMO_USERNAME_*` pattern) — existing demo users
  (`demo`, `demo-two`, `demo-three`) are untouched so current Playwright/E2E
  journeys keep working unmodified.

### `quotes` (new migration, `V6__add_owner_to_quotes.sql`)

```sql
ALTER TABLE quotes ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);
CREATE INDEX idx_quotes_user_id ON quotes (user_id);
```

- `Quote` domain model and JPA entity gain `userId` / `user_id`.
- `Quote.createDraft(...)` takes an additional `ownerId` parameter.

## 4. AuthN/AuthZ mechanics

- `TokenService.issueApiToken` changes from taking a `username` to taking the
  full `User`, so it can embed two new JWT claims: `uid` (the user's UUID as
  a string) and `role` (`USER`/`ADMIN`). Both existing call sites
  (`LoginService.issuePair`, `AuthController.refresh`) already have the
  `User` in scope, so this isn't a new lookup.
- `SecurityConfig`'s JWT authority mapping is extended with a converter that
  also turns the `role` claim into `ROLE_ADMIN`/`ROLE_USER`, alongside the
  existing `scope` → `SCOPE_api` mapping. The existing
  `.anyRequest().hasAuthority("SCOPE_api")` gate is unchanged; the role
  authority is purely additive and checked inside the application layer, not
  at the filter-chain level (mutation vs. read-only admin behavior needs
  method-level nuance the filter chain can't express).
- `QuoteController` and `SubmissionController` read
  `@AuthenticationPrincipal Jwt jwt` (the same pattern already used in
  `AuthController`) and build a `RequestingUser(UUID id, boolean admin)`
  record from the `uid`/`role` claims, passed into every `QuoteApi` /
  `SubmissionApi` call.

## 5. Application/domain layer changes

`QuoteApi` and `SubmissionApi` method signatures gain a `RequestingUser`
parameter:

```
create(command, requester)
updateCoverage(id, command, requester)          // always owner-scoped
getQuote(id, requester)                          // admin sees any quote
listQuotes(query, requester)                     // admin sees all
getSummary(requester)                            // admin sees global stats
ensureSubmittable(id, requester)                 // always owner-scoped
markSubmitted(id, requester)                     // always owner-scoped
markSubmissionFailed(id, requester)               // always owner-scoped
submit(quoteId, requester)                        // SubmissionApi, owner-scoped
```

`QuoteService` picks the scoped or unscoped repository call based on
`requester.admin()` only for the three read methods; every write path always
calls the owner-scoped repository method regardless of role.

## 6. Repository layer changes

`JpaQuoteRepository.findPage` already builds a `Specification<Quote>`
conjunctively (status/coverage/search predicates added only when present) —
an owner predicate is added the same way, only when the requester isn't an
admin. This fits the existing pattern with no new abstraction.

`findById` gets a sibling `findByIdAndUserId(id, ownerId)`; `QuoteService`
calls the owner-scoped one unless the requester is an admin doing a read.

`findSummary`'s per-status/per-coverage counters
(`countByStatus`, `countByCoverageType`, `sumMonthlyPremium`,
`averageMonthlyPremium`, `findTrendRows`) get owner-scoped sibling derived
queries (`countByStatusAndUserId`, etc.), selected the same way the existing
unscoped ones are called today — no dynamic query builder needed here since
Spring Data derived queries already cover it.

## 7. Cache correctness (bug this design closes)

`getQuote` is currently `@Cacheable(cacheNames = QUOTES_CACHE, key = "#id")`.
Once ownership matters, that key is unsafe on its own: if an admin fetches
quote X unscoped and it's cached under key `X`, a later request for the same
ID from a non-owner would be served the cached value directly by Spring's
cache abstraction — **before** the ownership check ever executes, since a
cache hit short-circuits the method body. The key changes to
`"#id + '|' + #requester.id()"` so cache entries are isolated per requester,
not just per quote. This is being fixed as part of this change, not filed
separately, since shipping ownership without it would silently reopen the
leak it's meant to close.

## 8. Testing

- Every existing `QuoteService`/`SubmissionService`/controller test that
  currently calls e.g. `quoteApi.getQuote(id)` needs a `RequestingUser`
  argument threaded through — mechanical, but touches most of the existing
  quote/submission suite.
- New coverage: cross-user 404 on `getQuote`/`updateCoverage`/`submit`;
  admin sees all quotes in `listQuotes`/`getSummary` but is still
  owner-scoped on every mutation; cache-key isolation between two different
  requesters requesting the same quote ID (regression test for §7).
- Repository slice tests (`@DataJpaTest`) for the new
  `findByIdAndUserId`/`...AndUserId` derived queries and the `Specification`
  owner predicate.

## 9. Documentation follow-up (not part of this change's code, tracked here so it isn't lost)

Once implemented, `docs/use_cases/UC-003-request-an-insurance-quote.md`,
`UC-004-submit-quote-to-insurer.md`, and
`UC-005-review-quote-history-and-analytics.md`, plus `docs/entity_model.md`,
need updating to reflect: `QUOTE.user_id`, the `USER.role` attribute, the new
Administrator actor, and the ownership/admin-read-only business rules
described in §2 and §5 above (would be BR-023 onward, continuing the
existing sequential numbering).

## 10. Out of scope

- Connection pool / server thread concurrency tuning — separate design,
  explicitly deferred to be brainstormed after this one.
- Any UI change in `insurance-quotes-web` to expose the admin view — not
  requested; this design is backend-only. If an admin UI is wanted later,
  it's a separate frontend design.
