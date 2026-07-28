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
- `SecurityConfig`'s filter chain is unchanged — `.anyRequest().hasAuthority("SCOPE_api")`
  keeps gating access exactly as today. Admin-awareness is purely an
  application-layer concern, not a Spring Security authority: `QuoteController`
  reads the `role` claim directly off the already-validated `Jwt` principal
  (`@AuthenticationPrincipal Jwt jwt`, the same pattern `AuthController`
  already uses) rather than mapping it to a `ROLE_ADMIN` `GrantedAuthority`.
  There is no `@PreAuthorize`/method-security use of this claim anywhere, so
  adding an authority mapping would be unused machinery.
- `QuoteController` builds a `RequestingUser(UUID id, boolean admin)` record
  (`quote.api.usecase.RequestingUser`) from the `uid`/`role` claims, but only
  passes it into the three read methods that are admin-aware
  (`getQuote`, `listQuotes`, `getSummary`). `create` and `updateCoverage`
  take a plain `UUID ownerId` (`requester.id()`), and `SubmissionController`
  extracts just the `uid` claim as a plain `UUID ownerId` — neither ever sees
  the `admin` flag, so it's structurally impossible for an admin-only bypass
  to leak into a mutation path by accident.

## 5. Application/domain layer changes

`QuoteApi` methods split by whether admin bypass can ever apply — encoded in
the parameter type itself, not just a runtime check, so a mutation path can't
accidentally be wired to the admin-aware overload:

```
create(command, ownerId)                    // UUID ownerId — always self
updateCoverage(id, command, ownerId)         // UUID ownerId — always owner-scoped
getQuote(id, requester)                      // RequestingUser — admin sees any quote
listQuotes(query, requester)                 // RequestingUser — admin sees all
getSummary(requester)                        // RequestingUser — admin sees global stats
getOwnedQuote(id, ownerId)                   // UUID ownerId — always owner-scoped
ensureSubmittable(id, ownerId)               // UUID ownerId — always owner-scoped
markSubmitted(id, ownerId)                   // UUID ownerId — always owner-scoped
markSubmissionFailed(id, ownerId)            // UUID ownerId — always owner-scoped
```

`SubmissionApi.submit(quoteId, ownerId)` takes a plain `UUID ownerId` too —
the submission flow never sees the `admin` flag at all, so there's no path
by which submitting a quote can be admin-bypassed. This replaces
`SubmissionService`'s current use of `quoteApi.getQuote(quoteId)` for its
idempotency check with the new owner-scoped `getOwnedQuote`.

`QuoteRepository` (the port) gains a nullable `UUID ownerId` parameter on its
read methods (`findById`, `findPage`, `findSummary`): `null` means unscoped
(admin), non-null means scoped to that owner. `QuoteService` passes
`requester.admin() ? null : requester.id()` for the three admin-aware
methods, and always the caller-supplied `ownerId` (never null) for every
owner-scoped method.

## 6. Repository layer changes

`JpaQuoteRepository.findPage(query, ownerId)` already builds a
`Specification<Quote>` conjunctively (status/coverage/search predicates
added only when present) — an owner predicate is added the same way, only
when `ownerId != null`. This fits the existing pattern with no new
abstraction.

`JpaQuoteRepository.findById(id, ownerId)` delegates to a new
`SpringDataQuoteRepository.findByIdAndUserId(id, ownerId)` derived query
when `ownerId != null`, otherwise the existing unscoped `delegate.findById(id)`.

`findSummary(now, ownerId)`'s per-status/per-coverage counters get owner-scoped
sibling derived queries on `SpringDataQuoteRepository`
(`countByStatusAndUserId`, `countByCoverageTypeAndUserId`,
`countByMonthlyPremiumIsNotNullAndUserId`, `sumMonthlyPremiumForUser`,
`averageMonthlyPremiumForUser`, `findTrendRowsForUser`), and
`JpaQuoteRepository` picks the scoped or unscoped method per counter based on
whether `ownerId` is null — no dynamic query builder needed here since
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
leak it's meant to close. Since `RequestingUser.id()` is always the real
authenticated caller's own UUID (admin or not — the `admin` boolean is a
separate flag, `id()` never changes meaning), `key = "#id + '|' + #requester.id()"`
is sufficient on its own: it isolates every requester's cache entries from
every other requester's, admin included, with no special-casing needed in
the key expression itself.

The three `@CacheEvict(key = "#id")` annotations on mutation paths
(`updateCoverage`, `markSubmitted`, `markSubmissionFailed`) become
`key = "#id + '|' + #ownerId"`, matching the owner's own cache entry exactly
(these methods are always owner-scoped, so `#ownerId` is the parameter name
directly, not `#requester.id()`). An admin who separately viewed that same
quote keeps a stale cached copy for up to the existing 10-minute TTL — a
staleness window, not a leak, since the admin was already authorized to see
that data; solving it would need cache tagging this codebase doesn't have
today, so it's left as-is.

**A second eviction path exists and must change too.** `QuoteCacheEvictionListener`
(`quote/adapter/in/messaging/consumer`) listens for the in-memory `QuoteExpired`
event fired by `DraftExpirationJob` and evicts the cache by `event.quoteId()`
alone. Once the cache key includes the owner, that plain-ID eviction stops
matching any real entry — draft expiration would silently stop invalidating
the cache for every requester, not just admins. Fix: `QuoteExpired` gains an
`ownerId` field (`record QuoteExpired(UUID quoteId, UUID ownerId)`);
`QuoteRepository.findIdsToExpire` is replaced by a
`findStaleDrafts(Instant cutoff)` returning `List<StaleQuoteRef>` (a new
`record StaleQuoteRef(UUID id, UUID ownerId)` in the port package) so
`DraftExpirationJob` has the owner available when it publishes each event;
`markExpired` keeps taking a plain `List<UUID>` extracted from those refs.
`QuoteCacheEvictionListener` evicts `event.quoteId() + "|" + event.ownerId()`.

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
