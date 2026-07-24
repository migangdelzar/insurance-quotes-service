# Redis and Responsive UI Hardening Design

| Field | Detail |
|---|---|
| Date | 2026-07-24 |
| Scope | Backend Redis state/cache and frontend responsive visual hardening |
| Repositories | `insurance-quotes-service`, `insurance-quotes-web` |
| Status | Approved design; implementation plan pending |

## 1. Goal

Replace instance-local Caffeine state with shared Redis state where horizontal scaling requires it, while keeping PostgreSQL as the durable source of truth. Audit and improve the frontend at representative viewport sizes so layout behavior is intentional for forms, steppers, action rows, lists, translated labels, and authentication states.

## 2. Decisions and boundaries

### Redis responsibilities

Redis will replace both current Caffeine uses:

1. `quotes` Spring Cache entries will be shared across service instances and invalidated through the existing cache annotations/event flow.
2. WebAuthn registration/assertion ceremonies will be stored as short-lived typed JSON values so a start request and finish request may be handled by different instances.

Redis is not the source of truth for users, passkeys, refresh-token rotation, quote aggregates, or business events. Those remain in PostgreSQL/Kafka-backed application flows. No distributed lock is introduced; one-time ceremony consumption uses Redis `GETDEL`, and database transactions preserve durable business invariants.

### Frontend responsibilities

The frontend remains MUI-based and capability-organized. The pass is limited to responsive layout, visual hierarchy, overflow, focus visibility, state presentation, and locale selection/propagation. It does not change business behavior, quote API payloads, or the public i18n API (`t()` and typed `tid()`).

### Locale detection and propagation

The browser will select the initial locale from `navigator.languages`/`navigator.language`, normalized to the supported `en-US` and `es-MX` locales. Unsupported or unavailable browser preferences fall back to `en-US`. The selected locale initializes `i18next` and is sent on every service request as `Accept-Language`, so localized API errors use the same locale as the visible web UI. No locale is persisted as a new server-side session concern; a future language switcher may explicitly change and persist the client preference.

## 3. Backend architecture

### Ports and adapters

The WebAuthn application flow will depend on a framework-neutral outbound port:

```java
// auth/application/port/out/WebAuthnCeremonyStore.java
public interface WebAuthnCeremonyStore {

    void save(String challengeId, StoredCeremony ceremony, Duration ttl);

    Optional<StoredCeremony> take(String challengeId);
}
```

The stored value is a separate port-level type:

```java
// auth/application/port/out/StoredCeremony.java

public record StoredCeremony(CeremonyType type, String payload) {

    public enum CeremonyType { REGISTRATION, ASSERTION }
}
```

The Redis adapter will use `StringRedisTemplate` with keys under `auth:webauthn:ceremony:`. Values contain a type discriminator and the Yubico JSON payload. The adapter will apply the five-minute TTL and consume values with `ValueOperations.getAndDelete`, which maps to Redis `GETDEL`. Missing, expired, malformed, or already-consumed values produce the existing framework-neutral `InvalidPasskeyException`.

`YubicoPasskeyAdapter` remains the translation boundary: it serializes `PublicKeyCredentialCreationOptions` or `AssertionRequest` before storage and reconstructs them with their `fromJson` methods after retrieval. Yubico classes do not cross the Redis port.

### Quote cache

Spring’s cache abstraction remains at `QuoteService`. The cache provider changes to Redis with:

- explicit `spring.cache.type=redis`;
- a ten-minute quote-entry TTL;
- key prefixes enabled;
- JSON value serialization suitable for `QuoteView` records;
- no cache of null values.

The cache is an optimization only. Cache misses load PostgreSQL, and all existing `@CacheEvict` annotations remain authoritative after mutations. A dedicated cache error handler logs Redis read/write failures and fails open to the PostgreSQL path; a Redis outage must not make an otherwise healthy quote read unavailable.

### Runtime configuration

Add `spring-boot-starter-data-redis`, remove the Caffeine dependency/configuration, and configure `spring.data.redis.host`/`port` through environment-backed properties. Add a Redis 7 container with a health check to the base Compose stack so JVM, full-stack, E2E, and native overlays use the same dependency graph.

Integration-test container fixtures will add a reusable Redis container and register its dynamic host/port properties for Spring Boot tests that exercise caching or WebAuthn ceremony storage.

## 4. Frontend responsive design

### Baseline findings

The current login screenshot is centered and readable at desktop and mobile sizes, but the skip link is visually present outside keyboard focus. Most pages use vertical `Stack` composition; action rows, the horizontal stepper, long translated labels, and senior health questions need explicit narrow-screen behavior. Summary and quote-list information currently has no intentional responsive grid.

### Target rules

- Keep content within the existing `Container`, with responsive horizontal padding and no horizontal page overflow.
- Hide the skip link visually until focus while preserving it in the accessibility tree.
- Use responsive `Stack` direction and full-width actions below the small breakpoint; preserve an inline action row at larger widths.
- Reflow the wizard progress indicator at narrow widths so labels never clip or force horizontal scrolling.
- Use MUI responsive grid/flex rules for summary rows and quote-list entries; long names, coverage labels, and translated strings must wrap.
- Preserve visible keyboard focus, logical tab order, form labels, error alerts, and heading focus after route changes.

### Browser audit matrix

The audit will cover 320px, 375px, 768px, and 1280px viewport widths and these states:

| Area | States |
|---|---|
| Authentication | password login, MFA/passkey prompt, enrollment dialog |
| Quote flow | personal form, standard coverage, senior health questions, summary |
| Outcomes | submission failure/retry, quotes list, empty/loading/error states |

Each state must be checked for horizontal overflow, clipped controls, wrapping, focus visibility, console errors, and network failures. Screenshots will be captured before and after targeted changes; persistent Playwright assertions will cover viewport overflow and critical control visibility.

## 5. Failure handling and operational behavior

| Scenario | Expected behavior |
|---|---|
| Redis unavailable during quote read | Log the cache failure and continue through PostgreSQL; the cache is best-effort and the failure is observable. |
| Redis unavailable during WebAuthn start/finish | Ceremony request fails with the existing passkey error; no partial credential is persisted. |
| Ceremony TTL expires | `InvalidPasskeyException`; user restarts the ceremony. |
| Ceremony is submitted twice | First `GETDEL` consumes it; subsequent request is rejected without a lock. |
| Browser width below 768px | Controls stack/wrap, no horizontal document overflow, focus remains visible. |
| Browser locale is unsupported or unavailable | Use `en-US` for both UI translations and the `Accept-Language` request header. |
| Browser locale is `es`, `es-MX`, `en`, or `en-US` | Normalize to the supported `es-MX` or `en-US` locale and use it consistently in the UI and API requests. |

## 6. Test strategy

Backend:

- Unit-test the ceremony store contract with a fake store and the Yubico adapter’s serialization/type mapping.
- Integration-test Redis TTL, JSON round-trip, atomic take-once behavior, and quote-cache population/eviction with Testcontainers Redis.
- Verify Compose health and API startup with Redis present.

Frontend:

- Keep existing component tests for behavior.
- Unit-test locale normalization and request-header propagation, including unsupported and regional browser locale values.
- Add Playwright responsive checks at the four viewport widths for document overflow and critical selectors.
- Capture reviewed screenshots for each major state; do not add brittle pixel snapshots unless a stable baseline is intentionally chosen.

## 7. Documentation

Add an ADR explaining why Redis is shared ephemeral infrastructure for serverless/horizontally scaled instances, why PostgreSQL remains durable auth/business state, why no distributed lock is required, and which Redis failure modes are acceptable. Update backend README, Compose instructions, and the frontend responsive verification section.

## 8. Authoritative references

- [Spring Boot Redis support](https://docs.spring.io/spring-boot/reference/data/nosql.html)
- [Spring Boot Redis cache configuration](https://docs.spring.io/spring-boot/4.0/reference/io/caching.html)
- [Spring Data Redis cache serialization](https://docs.spring.io/spring-data/redis/reference/4.0/redis/redis-cache.html)
- [Redis GETDEL](https://redis.io/docs/latest/commands/getdel/)
