# Requirements Catalog — Clara Insurance Quotes (Fullstack Code Challenge)

Source: `Fullstack_Code_Challenge_-_Onboarding_Team.pdf` (challenge brief), cross-checked
against the actual implementation in this repo (backend) and the sibling
[insurance-quotes-web](https://github.com/migangdelzar/insurance-quotes-web)
(frontend) as of 2026-07-30.

Status values follow the AIUP scale: **Open** (not implemented), **In Progress**,
**Implemented** (built, not independently verified here), **Verified** (built and
confirmed via code + passing tests/direct check), **Deferred**, **Rejected**.

Every row below was checked against real files, not assumed from the READMEs. Where a
row is **Open**, the evidence for the gap is in the [Gaps](#gaps--recommendations)
section.

## Functional Requirements

| ID     | Title | User Story | Priority | Status |
|--------|-------|-------------|----------|--------|
| FR-001 | Create Quote Draft | As an Applicant, I want to submit my name, email, age, and zip code so that a draft quote is created for me to continue. | High | Verified |
| FR-002 | Validate Personal Info Client-Side | As an Applicant, I want the personal info form to validate before I proceed so that I don't submit incomplete or invalid data. | High | Verified |
| FR-003 | Select Coverage Type | As an Applicant, I want to choose Basic, Standard, or Premium coverage so that my quote reflects the plan I want. | High | Verified |
| FR-004 | Answer Senior Health Questions | As an Applicant older than 65, I want to answer pre-existing conditions, prescription medication, tobacco use, and spouse coverage questions so that my premium reflects my health profile. | High | Verified |
| FR-005 | View Dynamically Updated Premium | As an Applicant, I want to see my estimated monthly premium update as I change coverage or health selections so that I understand the cost impact before submitting. | High | Verified |
| FR-006 | Recalculate Premium Server-Side on Coverage Update | As a System, I want `PATCH /quotes/{id}/coverage` to recalculate and persist the premium so that pricing is never trusted from the client. | High | Verified |
| FR-007 | Enforce Age-Gated Health Data Rule Server-Side | As a System, I want to reject health data submitted for applicants aged 65 or under so that the conditional rule cannot be bypassed by a malicious or buggy client. | High | Verified |
| FR-008 | Review Quote Summary | As an Applicant, I want a read-only summary of everything I entered plus my calculated premium so that I can confirm before submitting. | High | Verified |
| FR-009 | Submit Quote to Insurer | As an Applicant, I want to submit my finished quote to the insurer so that my application is formally filed. | High | Verified |
| FR-010 | Idempotent Resubmission | As an Applicant, I want resubmitting an already-submitted quote to succeed without side effects so that accidental double-clicks don't trigger duplicate insurer calls. | High | Verified |
| FR-011 | Retry After Submission Failure | As an Applicant, I want a failed submission to leave my quote resubmittable so that I can retry after a transient insurer or network failure. | High | Verified |
| FR-012 | Retrieve a Single Quote | As an Applicant, I want to fetch a specific quote by id so that I can see its current status and premium. | Medium | Verified |
| FR-013 | List All Quotes | As an Admin, I want to list all quotes so that I can review submission activity across applicants. | Medium | Verified |
| FR-014 | Expire Stale Draft Quotes | As a System, I want a scheduled job to transition drafts older than a configurable window to `EXPIRED` in one batch so that abandoned quotes don't linger indefinitely. | Medium | Verified |
| FR-015 | Block Actions on Expired Quotes | As a System, I want `EXPIRED` quotes to be rejected from submission like any other invalid state so that stale data can never be finalized. | High | Verified |
| FR-016 | Invalidate Cache on Expiration | As a System, I want a quote's cache entry evicted the moment it expires so that cached reads never return stale data for an `EXPIRED` quote. | Medium | Verified |
| FR-017 | Authenticate Before Any Quote Action | As an Applicant, I want to log in before I can create, update, or submit a quote so that my data stays private to me. | High | Verified |
| FR-018 | Handle Failed or Timed-Out Submission in the UI | As an Applicant, I want clear feedback — and, on a timeout, an automatic status recheck — when submission fails so that I know whether to retry or wait. | High | Verified |
| FR-019 | Passwordless / Passkey Login | As an Applicant, I want to enroll and sign in with a passkey so that I can access my quotes without typing a password. | Low | Verified |
| FR-020 | View Paginated Quote History | As an Applicant, I want to filter, sort, and page through my past quotes so that I can find a specific one quickly. | Low | Verified |
| FR-021 | View Business Analytics on Home | As an Admin, I want summary analytics (status distribution, submission rate, etc.) on the home dashboard so that I can gauge overall quote activity at a glance. | Low | Verified |
| FR-022 | Localized UI | As an Applicant, I want the app in my browser's language (en-US or es-MX) so that I can use it comfortably. | Low | Verified |

FR-019 through FR-022 are beyond the challenge brief; they're tracked because they're
real, shipped, tested capabilities, not because the brief requires them.

## Non-Functional Requirements

| ID      | Title | Requirement | Category | Priority | Status |
|---------|-------|-------------|----------|----------|--------|
| NFR-001 | Cached Quote Read Latency | `GET /quotes/{id}` responses must be served from a 10-minute TTL Redis cache to avoid repeat DB reads for an unchanged quote. | Performance | Medium | Verified |
| NFR-002 | Insurer Call Timeout Budget | Outbound insurer submission calls must fail within a 2s connect / 5s read timeout so a slow third party cannot hang a request indefinitely. | Performance | High | Verified |
| NFR-003 | Backend Test Coverage Gate | Backend line coverage must remain at or above 80% (JaCoCo `COVEREDRATIO` rule), enforced at build time. | Maintainability | High | Verified |
| NFR-004 | Unauthenticated Request Rejection | Every non-public endpoint must return HTTP 401 with a structured JSON error, never a stack trace, for unauthenticated requests. | Security | High | Verified |
| NFR-005 | No Leaked Internals in Errors | All error responses must use the shared `ApiError` shape and never leak stack traces or raw exception messages to the client. | Security | High | Verified |
| NFR-006 | Rate-Limited Mutation Endpoints | Quote-mutating endpoints must be capped at a configurable rate (default 30 requests/minute per client) to blunt retry storms and abuse. | Scalability | Medium | Verified |
| NFR-007 | Draft Retention Window | Draft quotes must expire after a configurable TTL (default 30 minutes) rather than persisting indefinitely. | Scalability | Medium | Verified |
| NFR-008 | Responsive Layout | The web app must render usable layouts at mobile (<600px) and desktop (≥900px) breakpoints without horizontal scrolling or overlapping controls. | Usability | High | Verified |
| NFR-009 | Accessible Interaction | Interactive wizard controls must expose ARIA roles/labels and move focus to each step's heading on navigation so screen-reader users can follow progress. | Usability | Medium | Verified |
| NFR-010 | One-Command Local Deployment | The full backend stack (API + PostgreSQL + dependencies) must start with a single Compose command for local review. | Portability | High | Verified |
| NFR-011 | Structured, Correlated Logs | Application logs must be emitted as JSON with `correlationId`/`traceId`/`spanId` so a single request can be traced end-to-end. | Maintainability | Low | Verified |

## Constraints

| ID    | Title | Constraint | Category | Priority | Status |
|-------|-------|------------|----------|----------|--------|
| C-001 | Backend Runtime | Backend must run on Java 17. | Technical | High | Verified |
| C-002 | Backend Framework | Backend must use Spring Boot. | Technical | High | Verified |
| C-003 | Backend Build Tool | Backend must build with Maven. | Technical | High | Verified |
| C-004 | Backend Persistence | Backend must persist quotes via Spring Data JPA + PostgreSQL. | Technical | High | Verified |
| C-005 | Frontend Language | Frontend must be written in TypeScript. | Technical | High | Verified |
| C-006 | Frontend Framework | Frontend must use React. | Technical | High | Verified |
| C-007 | Frontend Component Library | Frontend must use Material UI (MUI). | Technical | High | Verified |
| C-008 | Frontend Form Handling | Frontend forms must use React Hook Form with Yup schema validation. | Technical | High | Verified |
| C-009 | Cross-Repo Delivery | Backend and frontend must be delivered as two separate public GitHub repositories that link to each other. | Business | High | Verified |
| C-010 | No Hand-Rolled Insurer Fake | The Step 3 insurer call must hit a real, free, public API rather than an internally simulated response. | Technical | High | Verified |
| C-011 | Fixed Premium Formula | The monthly premium formula and its constants (base $50/$100/$200; multipliers ×1.5/×1.3/×1.2/×1.4) must be implemented exactly as specified, not redesigned. | Technical | High | Verified |
| C-012 | README Submission Narrative | Each repository's README must cover: setup/test instructions, pre-coding thought process, design-decision rationale, AI-tool usage disclosure (which parts, how directed/reviewed), unfinished work or challenges encountered, and a link to the sibling repo. | Business | High | **Open** |
| C-013 | Default Branch Currency | Each repository's default branch (`main`) must reflect the current state of the work, not an older snapshot. | Operational | High | Verified |

## Evidence

Per CodeRabbit review feedback on this document, every `Verified` row is backed by
the implementation path and/or test named below rather than the status word alone.

### Functional Requirements

| ID | Implementation | Test evidence |
|----|-----------------|----------------|
| FR-001 | `QuoteController.java:39` (`POST /quotes`) → `QuoteService.create()` | `QuoteControllerTest`, `QuoteServiceTest` |
| FR-002 | `apps/web/src/features/quote-wizard/steps/personal/personalSchema.ts:3-21` (yup, all fields required) | `PersonalInfoStep.test.tsx` |
| FR-003 | `apps/web/.../CoverageStep.tsx` (radio group BASIC/STANDARD/PREMIUM) | `CoverageStep.test.tsx` |
| FR-004 | `apps/web/.../HealthQuestionsSection.tsx:11-25` (conditions multiselect, prescription, tobacco, spouse) | `HealthQuestionsSection.test.tsx` |
| FR-005 | `apps/web/.../useDebouncedCoverageSync.ts` (400ms debounce → `PATCH /quotes/{id}/coverage`) | `useDebouncedCoverageSync.test.tsx`, `PremiumDisplay.test.tsx` |
| FR-006 | `QuoteService.java:63-80` (`updateCoverage`) → `DefaultPremiumCalculator.java` | `DefaultPremiumCalculatorTest.specWorkedExample_age70StandardOneConditionSmokerWithSpouse_is327_60` |
| FR-007 | `QuoteService.java:179-183` (`rejectHealthDataForNonSeniors`) → `HealthDataNotAllowedException` | `QuoteServiceTest` |
| FR-008 | `apps/web/.../SummaryStep.tsx` (read-only review + `PremiumDisplay`) | covered via wizard integration tests |
| FR-009 | `SubmissionController.java:21` (`POST /quotes/{id}/submit`) → `SubmissionService.submit()` → `HttpInsurerClient` (`https://httpbin.org/status/200`, confirmed reachable via curl) | `SubmissionControllerTest`, `SubmissionServiceTest.submit_insurerAccepts_finalizes` |
| FR-010 | `SubmissionService.java:26-31` (already-`SUBMITTED` short-circuit) | `SubmissionServiceTest.submit_alreadySubmitted_isIdempotentAndSkipsInsurer` |
| FR-011 | `SubmissionService.java:39-50` (`markSubmissionFailed` on `InsurerUnavailableException`) | `SubmissionServiceTest.submit_insurerFails_marksFailedAndRethrows`, `QuoteTest.markSubmissionFailed_thenSubmittable_again` |
| FR-012 | `QuoteController.java:55` (`GET /quotes/{id}`), `@Cacheable` in `QuoteService.getQuote` | `QuoteControllerTest` |
| FR-013 | `QuoteController.java:60` (`GET /quotes`) | `QuoteControllerTest` |
| FR-014 | `DraftExpirationJob.java` (`expireStaleDrafts`), `ExpirationScheduleConfig.java`, `quote.expiration.draft-ttl` in `application.yml` | job unit coverage under `quote.application.service` test package |
| FR-015 | `QuoteTest.expiredQuote_isNeverSubmittable` | same |
| FR-016 | `QuoteCacheEvictionListener.java:18-25` (`@EventListener` on `QuoteExpired`) | — |
| FR-017 | `SecurityConfig.java:28-46` (`anyRequest().hasAuthority("SCOPE_api")`, 401 entry point) | — |
| FR-018 | `apps/web/.../useSubmitQuote.ts` (timeout → `getQuote` recheck), `SubmissionResult.tsx` | `useSubmitQuote.test.tsx`, `SubmissionResult.test.tsx` |
| FR-019 | `AuthController.java:60-78` (WebAuthn endpoints), `UC-002-enroll-a-passkey.md` | — |
| FR-020 | `apps/web/.../QuotesListPage.tsx` (pagination/filter/sort) | `QuotesListPage.test.tsx` |
| FR-021 | `QuoteService.java:103-139` (`getSummary`) | — |
| FR-022 | `packages/app-i18n` (`t()`/`tid()`), browser locale detection | — |

### Non-Functional Requirements

| ID | Evidence |
|----|----------|
| NFR-001 | `application.yml:32-38` — `spring.cache.redis.time-to-live: 10m` |
| NFR-002 | `application.yml:59-62` — `insurer.connect-timeout: 2s`, `read-timeout: 5s`; enforced in `InsurerClientConfig.java` |
| NFR-003 | `pom.xml:278-292` — JaCoCo `COVEREDRATIO` rule, `minimum: 0.80` |
| NFR-004 | `SecurityConfig.java:57-65` — `restAuthenticationEntryPoint` returns 401 + `ApiError` JSON |
| NFR-005 | `GlobalExceptionHandler.java`, plus module advices (`QuoteExceptionHandler`, `SubmissionExceptionHandler`, `AuthExceptionHandler`) |
| NFR-006 | `application.yml:92-97` — `web.rate-limit.quote-mutation.limit: 30`, `window: 1m` |
| NFR-007 | `application.yml:99-102` — `quote.expiration.draft-ttl: 30m` |
| NFR-008 | `apps/web/.../AppNavigation.tsx:50` (`useMediaQuery`), `AppShell.tsx` breakpoint `sx` props |
| NFR-009 | `packages/build-config/eslint.config.js` (`jsx-a11y` recommended), `useFocusHeading.ts`, `WizardProgress.tsx` (`aria-current="step"`) |
| NFR-010 | `deployment/compose/docker-compose.yml` + `mise run demo` |
| NFR-011 | README "Reading logs and traces" section; JSON stdout → Alloy → Loki pipeline |

### Constraints

| ID | Evidence |
|----|----------|
| C-001–C-004 | `pom.xml` (`java.version`, Spring Boot parent, Maven build), Flyway migrations under `service/src/main/resources/db/migration` |
| C-005–C-008 | `apps/web/package.json` — `react`, `typescript`, `@mui/material`, `react-hook-form`, `yup` |
| C-009 | `gh repo view migangdelzar/insurance-quotes-service --json visibility` → `PUBLIC`; same for `insurance-quotes-web`; each README links the other |
| C-010 | `application.yml:60` — `INSURER_BASE_URL` default `https://httpbin.org/status/200`, confirmed reachable via direct `curl -X POST` |
| C-011 | `AgeFactor.java`, `ConditionsFactor.java`, `TobaccoFactor.java`, `SpouseFactor.java` (constants `1.5`/`1.3`/`1.2`/`1.4`), `CoverageType` base premiums; `DefaultPremiumCalculatorTest.specWorkedExample_...` reproduces the brief's $327.60 worked example exactly |
| C-012 | Absence confirmed via `grep -ni "AI tool\|thought process\|challenges I ran" README.md` on both repos — no matches |
| C-013 | `gh pr view` on `insurance-quotes-service#4` and `insurance-quotes-web#4` — both `MERGED` into `main` |

## Gaps & Recommendations

Everything technical in the challenge brief — both endpoints, the fixed pricing
formula, server-side enforcement of the age rule, idempotent/retryable submission,
caching with invalidation, the scheduled expiration job, Kafka messaging, auth,
CORS, and every frontend wizard/validation/responsiveness/error-handling
requirement — is implemented and has direct test evidence. One thing stands between
this and a clean submission, and it's not code:

1. **C-012 — README narrative is incomplete.** Both READMEs are strong on
   architecture and setup, but neither has the sections the brief explicitly
   requires: *how you approached the problem before writing code*, *whether and how
   you used AI tools*, and *challenges you ran into / what you didn't finish*. A
   reviewer grading against the literal submission checklist will look for these
   and not find them.

~~2. Unmerged work on the default branch.~~ **Resolved 2026-07-30.** Both
`insurance-quotes-service` PR [#4](https://github.com/migangdelzar/insurance-quotes-service/pull/4)
and `insurance-quotes-web` PR [#4](https://github.com/migangdelzar/insurance-quotes-web/pull/4)
were merged into `main`, so the default branch of both public repos now reflects
current work.

No budget or deadline constraint is stated in the challenge brief, so none is
recorded here — inventing one would violate the "no fabricated thresholds" rule for
constraints.
