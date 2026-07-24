# Redis, Responsive UI, and Dynamic Locale Hardening

| Field | Detail |
|---|---|
| **Design specification** | [2026-07-24-redis-responsive-ui-design.md](../specs/2026-07-24-redis-responsive-ui-design.md) |
| **Repositories** | `insurance-quotes-service`, `insurance-quotes-web` |
| **Backend branch** | `feat-backend-core` |
| **Frontend branch** | `feat-frontend` |
| **Date** | 2026-07-24 |
| **Status** | In progress |

## 1. Summary

Replace both instance-local Caffeine usages with Redis-backed infrastructure: the quote cache becomes a shared, best-effort Redis cache, and WebAuthn ceremonies move behind a framework-neutral port with atomic one-time Redis consumption. PostgreSQL remains the durable source of truth, and no distributed lock is introduced.

In the web application, detect and normalize the browser locale to `en-US` or `es-MX`, send the selected locale as `Accept-Language` on every API request, and harden MUI layouts for narrow and wide viewports. Add deterministic responsive browser checks and update operational documentation.

## 2. Task list

### Phase A — Backend Redis foundation

| # | Task | Test file | Source/config files | Status |
|---|---|---|---|---|
| 1 | Add Redis dependency, environment-backed configuration, and Compose health-gated service. | `service/src/test/java/com/clara/insurancequotes/shared/configuration/RedisConfigurationTest.java` | `service/pom.xml`, `service/src/main/resources/application.yml`, `deployment/compose/docker-compose.yml`, `deployment/compose/docker-compose.jvm.yml`, native/full overlays | ✅ Done |
| 2 | Define the framework-neutral WebAuthn ceremony store port and typed stored ceremony value. | `service/src/test/java/com/clara/insurancequotes/auth/application/port/out/StoredCeremonyTest.java` | `service/src/main/java/com/clara/insurancequotes/auth/application/port/out/WebAuthnCeremonyStore.java`, `StoredCeremony.java` | ✅ Done |
| 3 | Implement the Redis ceremony adapter with namespaced keys, five-minute TTL, JSON values, and atomic `getAndDelete` take-once semantics. | `service/src/test/java/com/clara/insurancequotes/auth/adapter/out/cache/RedisWebAuthnCeremonyStoreIT.java` | `service/src/main/java/com/clara/insurancequotes/auth/adapter/out/cache/RedisWebAuthnCeremonyStore.java`, `service/src/testFixtures/java/com/clara/insurancequotes/testsupport/Containers.java` | ✅ Done |
| 4 | Refactor `YubicoPasskeyAdapter` to serialize only Yubico JSON at the port boundary and reconstruct registration/assertion requests after retrieval. | `service/src/test/java/com/clara/insurancequotes/auth/adapter/out/cache/RedisWebAuthnCeremonyStoreTest.java` | `service/src/main/java/com/clara/insurancequotes/auth/adapter/out/webauthn/YubicoPasskeyAdapter.java`, `service/src/main/java/com/clara/insurancequotes/auth/configuration/WebAuthnConfig.java` | ✅ Done |
| 5 | Switch Spring quote caching from Caffeine to Redis with ten-minute TTL, JSON `QuoteView` values, prefixes, no nulls, and fail-open cache error handling. | `service/src/integrationTest/java/com/clara/insurancequotes/quote/application/service/QuoteCachingIT.java`, `service/src/test/java/com/clara/insurancequotes/shared/cache/RedisCacheErrorHandlerTest.java` | `service/src/main/java/com/clara/insurancequotes/quote/configuration/CacheConfig.java`, `service/src/main/java/com/clara/insurancequotes/shared/cache/RedisCacheErrorHandler.java`, `service/src/main/resources/application.yml`, `service/pom.xml` | ✅ Done |
| 6 | Register Redis dynamically in Spring integration fixtures and update all full-context tests that require Redis. | Existing Spring Boot integration tests plus `service/src/testFixtures/java/com/clara/insurancequotes/testsupport/Containers.java` | `QuoteCachingIT`, `DraftExpirationJobIT`, `SubmissionFlowIT`, `OpenApiExportIT`, and related test property setup | ✅ Done |

### Phase B — Frontend dynamic locale

| # | Task | Test file | Source files | Status |
|---|---|---|---|---|
| 7 | Add pure browser-locale normalization/detection for `navigator.languages` and `navigator.language`, supporting `en`, `en-US`, `es`, and `es-MX` with `en-US` fallback. | `apps/web/src/app/locale.test.ts` | `apps/web/src/app/locale.ts`, `apps/web/src/app/i18n.ts` | ✅ Done |
| 8 | Propagate the selected locale through the HTTP client as `Accept-Language` on initial requests and refresh retries. | `apps/web/src/shared/api/httpClient.test.ts` | `apps/web/src/shared/api/httpClient.ts`, `apps/web/src/features/auth/context/AuthProvider.tsx` | ✅ Done |
| 9 | Verify the UI and API error translations use the same selected locale without changing the public `t()`/`tid()` API. | `apps/web/src/app/locale.test.ts`, `apps/web/src/shared/api/httpClient.test.ts` | `apps/web/src/app/i18n.ts`, `apps/web/src/shared/api/httpClient.ts`, `apps/web/src/features/auth/context/AuthProvider.tsx` | ✅ Done |

### Phase C — Responsive UI hardening

| # | Task | Test file | Source files | Status |
|---|---|---|---|---|
| 10 | Centralize responsive breakpoints/spacing and hide the skip link until keyboard focus. | `e2e/tests/responsive-layout.spec.ts` | `apps/web/src/app/App.tsx`, `apps/web/src/shared/theme/theme.ts` | ✅ Done |
| 11 | Make authentication, wizard actions, summary rows, quote-list rows, and senior health controls wrap/stack without horizontal overflow. | `e2e/tests/responsive-layout.spec.ts`, existing component tests | `apps/web/src/features/auth/**`, `apps/web/src/features/quote-wizard/**`, `apps/web/src/pages/QuotesListPage.tsx` | ✅ Done |
| 12 | Reflow the wizard progress indicator and apply responsive MUI Grid/Flex layout to summary/list information while preserving labels, focus, and tab order. | `e2e/tests/responsive-layout.spec.ts`, existing component tests | `apps/web/src/features/quote-wizard/components/WizardProgress.tsx`, `apps/web/src/features/quote-wizard/steps/summary/SummaryStep.tsx` | ✅ Done |
| 13 | Add browser-level responsive checks at 320, 375, 768, and 1280 pixels for overflow, clipping, critical controls, and keyboard focus; locale propagation is covered by HTTP-client tests. | `e2e/tests/responsive-layout.spec.ts` | `e2e/tests/responsive-layout.spec.ts` | ✅ Done |

### Phase D — Documentation and verification

| # | Task | Test file | Source/docs files | Status |
|---|---|---|---|---|
| 14 | Document Redis responsibilities, serverless/horizontal-scaling rationale, failure modes, local operations, dynamic locale behavior, and responsive verification commands. | Documentation validation via repository checks | `docs/decisions/ADR-011-redis-shared-ephemeral-state.md`, backend `README.md`, frontend `README.md`, Compose documentation | ✅ Done |
| 15 | Run all backend tests/builds, frontend unit/type/lint tests, Compose startup checks, and browser responsive audit; record limitations such as unavailable local Testcontainers/native resources. | Full suites and browser audit | Plan status and verification notes | ✅ Done |

## 3. TDD execution protocol

For every implementation task:

1. Write the smallest focused test first and run it to confirm the expected failure.
2. Implement the minimum behavior needed to pass that test.
3. Refactor only after the test is green, then rerun the focused and relevant full suite.
4. Mark the task status through 🔴 Test Written, 🟢 Test Passing, 🔵 Refactored, and ✅ Done.
5. Commit each coherent task with a conventional commit message and push the affected repository branch before continuing.

Integration tests must use the existing Testcontainers fixture with Redis rather than a developer-local Redis dependency. Browser checks must use typed `tid()` selectors and semantic assertions; pixel screenshots are review artifacts, not brittle pass/fail snapshots.

## 4. Dependencies and ordering

```text
1 → 2 → 3 → 4
1 → 5 → 6
7 → 8 → 9
10 → 11 → 12 → 13
3, 5, 6, 9, 13 → 14 → 15
```

The backend Redis container/configuration must exist before ceremony and quote-cache integration tests. Locale normalization must exist before request-header propagation. Responsive primitives should be established before page-level layout changes. Documentation and final verification happen after both repositories are functionally complete.

## 5. Integration points

- `QuoteService` keeps its existing cache annotations and PostgreSQL repository behavior.
- `YubicoPasskeyAdapter` remains the only component that knows Yubico request serialization types.
- `GlobalExceptionHandler` continues selecting API error messages from `Accept-Language`.
- The web app continues using the existing `t()` and typed `tid()` contracts.
- Compose JVM/native/full-stack overlays share one Redis service and health dependency.
- Existing public draft PRs receive the pushed commits on `feat-backend-core` and `feat-frontend`.

## 6. Verification results

- Backend `TESTCONTAINERS_RYUK_DISABLED=true mise exec -- mvn -pl service verify` passed: unit tests, Redis-backed integration tests, Spotless, and JaCoCo completed successfully.
- Frontend `bunx vitest run` passed with 32 tests; `bun run build` passed; `bun run --filter web lint` passed with only the repository's existing fast-refresh warnings.
- Responsive browser audit passed at 320, 375, 768, and 1280 pixels using `E2E_BASE_URL=http://localhost:3100 bunx playwright test tests/responsive-layout.spec.ts --project=desktop-chromium`.
- Compose configuration validated and the Redis 7 service reached healthy status locally.
- The pre-existing full E2E journey suite was not completed because the shared demo database already contains passkey/MFA state; a clean E2E database is required for that suite.
- A fresh Docker API image rebuild was not completed because the local Docker Buildx/Maven build stage was unavailable; Maven verification and Compose validation remain green.

## 7. Definition of done

- [x] Both Caffeine usages are removed from runtime configuration and dependencies.
- [x] Redis-backed quote cache and WebAuthn ceremony store are covered by focused and integration tests.
- [x] WebAuthn ceremony consumption is atomic and one-time without distributed locks.
- [x] PostgreSQL remains the durable source of truth.
- [x] Browser locale detection, normalization, UI initialization, and `Accept-Language` propagation are tested.
- [x] Responsive layouts pass the four viewport checks without horizontal overflow or clipped critical controls.
- [x] Keyboard focus, heading focus, labels, errors, and tab order remain usable.
- [x] ADR, README, Compose, and verification documentation are updated.
- [x] Backend and frontend tests/builds pass, or environmental limitations are documented with evidence.
- [ ] All changed files are committed and pushed to their existing feature branches.
- [x] The plan status is `Complete` only after final verification.
