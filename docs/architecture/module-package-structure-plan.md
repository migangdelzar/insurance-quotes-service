# Modulith package structure implementation plan

> **For agentic workers:** Execute this plan task-by-task with
> `superpowers:subagent-driven-development`. Each task must be implemented,
> tested, reviewed, committed, and marked complete before the next task starts.

**Goal:** Apply one consistent DDD + hexagonal package and class-naming convention to every Spring Modulith module without changing the HTTP, persistence, or business contracts.

**Architecture:** Keep `api` as the only cross-module contract, place orchestration in `application`, business rules in `domain`, and all transport/technology code in `adapter.in` or `adapter.out`. Replace broad `Api` facades and transport DTOs in `api` with focused use-case contracts and adapter-owned request models.

**Tech Stack:** Java 17, Spring Boot, Spring Modulith, Maven, JUnit 5, ArchUnit/Spring Modulith module verification, Lombok, Spring MVC, JPA, Redis, Kafka, and Micrometer.

## Global Constraints

- Preserve all existing HTTP paths, API version values, JSON property names, validation behavior, and status codes.
- Preserve PostgreSQL tables/migrations, Kafka event payloads, Redis keys, cache names, and environment-variable names.
- Domain packages must not depend on Spring MVC, JPA, Kafka, Redis, HTTP clients, JSON, or actuator classes.
- Other modules may use only named API packages or explicitly public shared contracts; never another module's `application`, `domain`, or `adapter` package.
- Every `UseCase` interface must have a matching `<Verb><Noun>Service` implementation unless the existing class is a deliberately named domain policy or technical component.
- HTTP DTOs use `Request`/`Response`; application results use business names such as `Details`, `Summary`, `Page`, or `Result`.
- Events use past tense; commands use imperative tense.
- Add `package-info.java` documentation for every package that exists; do not create empty packages for symmetry.
- Use TDD for behavior-preserving changes: add or update a regression test before each implementation rename/split, run the focused test, then the relevant module suite.
- Keep commits small and conventional: `refactor(module): ...`, `test(module): ...`, `docs(architecture): ...`.

---

## Task 1: Establish module metadata and package vocabulary

**Files:**

- Create: `service/src/main/java/com/clara/insurancequotes/auth/package-info.java`
- Create: `service/src/main/java/com/clara/insurancequotes/pricing/package-info.java`
- Create: `service/src/main/java/com/clara/insurancequotes/quote/package-info.java`
- Create: `service/src/main/java/com/clara/insurancequotes/submission/package-info.java`
- Create/update: package-info files below every existing `api`, `application`, `domain`, `adapter`, `configuration`, and `shared` responsibility package.
- Modify: `service/src/test/java/com/clara/insurancequotes/ModularityTest.java`

**Interfaces:**

- Produces module display names and named interfaces for public API subpackages.
- Keeps existing module dependency behavior unchanged.

- [ ] **Step 1: Write the failing architecture assertions**

Add tests that assert the application discovers `auth`, `pricing`, `quote`,
`submission`, and `shared` as modules and that public named interfaces include
`quote-api-command`, `quote-api-query`, `quote-api-result`,
`quote-api-usecase`, `quote-api-event`, `quote-api-exception`, and
`quote-api-type`.

- [ ] **Step 2: Run the focused test and verify it fails**

Run:

```bash
mvn -pl service -Dtest=ModularityTest test
```

Expected: the new named-interface assertions fail because the package metadata
does not exist yet.

- [ ] **Step 3: Add metadata and package documentation**

Use `@ApplicationModule(displayName = "Quote")` on module package-info files
and `@NamedInterface("quote-api-usecase")`-style annotations on public API
subpackages. Each package-info must explain what belongs there and what must
not be imported.

- [ ] **Step 4: Run the focused test and module verification**

Run:

```bash
mvn -pl service -Dtest=ModularityTest test
```

Expected: PASS and regenerated module documentation reflects the same package
names.

- [ ] **Step 5: Commit**

```bash
git add service/src/main/java service/src/test/java/com/clara/insurancequotes/ModularityTest.java
git commit -m "docs(modulith): document module package responsibilities"
```

## Task 2: Move shared configuration and observability by responsibility

**Files:**

- Rename: `service/src/main/java/com/clara/insurancequotes/config/BusinessMetrics.java` → `service/src/main/java/com/clara/insurancequotes/shared/observability/BusinessMetrics.java`
- Rename: `service/src/main/java/com/clara/insurancequotes/config/CorrelationIdFilter.java` → `service/src/main/java/com/clara/insurancequotes/shared/adapter/in/web/filter/CorrelationIdFilter.java`
- Rename: `service/src/main/java/com/clara/insurancequotes/config/OpenApiConfig.java` → `service/src/main/java/com/clara/insurancequotes/shared/configuration/OpenApiConfiguration.java`
- Rename: `service/src/main/java/com/clara/insurancequotes/config/WebVersioningConfig.java` → `service/src/main/java/com/clara/insurancequotes/shared/configuration/WebVersioningConfiguration.java`
- Modify: all imports and tests referring to `com.clara.insurancequotes.config.*`
- Create: package-info files for `shared.observability` and `shared.adapter.in.web.filter`

**Interfaces:**

- Consumes: existing Spring beans and public module contracts.
- Produces: the same beans under responsibility-based packages.

- [ ] **Step 1: Update package-level regression checks**

Extend architecture tests to reject `com.clara.insurancequotes.config` and to
require `BusinessMetrics` under `shared.observability` and web filtering under
`shared.adapter.in.web.filter`.

- [ ] **Step 2: Run the focused test and verify it fails**

Run:

```bash
mvn -pl service -Dtest=ModularityTest,BusinessMetricsTest,CorrelationIdFilterTest,PerformanceConfigurationTest test
```

Expected: failure because the old packages still exist.

- [ ] **Step 3: Move and rename the classes**

Update package declarations, imports, Spring annotations, test package names,
and generated documentation references. Do not change bean names, metric names,
filter behavior, or configuration properties.

- [ ] **Step 4: Run focused tests**

Run the command from Step 2 and expect all tests to pass.

- [ ] **Step 5: Commit**

```bash
git add service/src/main/java service/src/test/java docs/architecture/modules
git commit -m "refactor(shared): align cross-cutting package responsibilities"
```

## Task 3: Standardize pricing commands and use-case naming

**Files:**

- Rename: `pricing/api/command/PricingInput.java` → `pricing/api/command/CalculatePremiumCommand.java`
- Rename: `pricing/api/usecase/PremiumCalculator.java` → `pricing/api/usecase/CalculatePremiumUseCase.java`
- Rename: `pricing/application/service/DefaultPremiumCalculator.java` → `pricing/application/service/CalculatePremiumService.java`
- Modify: pricing tests, quote application imports, and package-info documentation.

**Interfaces:**

```java
public interface CalculatePremiumUseCase {
    Premium calculate(CalculatePremiumCommand command);
}
```

`CalculatePremiumService` implements this interface and preserves the existing
factor calculation and `Premium` result.

- [ ] **Step 1: Update the pricing service test to use target names**
- [ ] **Step 2: Run `mvn -pl service -Dtest=DefaultPremiumCalculatorTest test` and verify the rename test does not compile**
- [ ] **Step 3: Rename the command/interface/service and update imports**
- [ ] **Step 4: Run `mvn -pl service -Dtest=DefaultPremiumCalculatorTest test` and the quote service tests; expect PASS**
- [ ] **Step 5: Commit**

```bash
git add service/src/main/java/com/clara/insurancequotes/pricing service/src/main/java/com/clara/insurancequotes/quote service/src/test/java/com/clara/insurancequotes/pricing
git commit -m "refactor(pricing): use consistent command and use-case names"
```

## Task 4: Split the quote API and rename public quote models

**Files:**

- Rename: `quote/api/query/QuoteQuery.java` → `SearchQuotesQuery.java`
- Rename: `quote/api/result/QuoteView.java` → `QuoteDetails.java`
- Rename: `quote/api/result/QuotePageView.java` → `QuotePage.java`
- Rename: `quote/api/result/QuoteSummaryView.java` → `QuoteSummary.java`
- Rename: `quote/api/result/QuoteDistributionView.java` → `QuoteDistribution.java`
- Rename: `quote/api/result/QuoteTrendPointView.java` → `QuoteTrendPoint.java`
- Rename: `quote/api/usecase/QuoteApi.java` → focused use-case interfaces in `quote/api/usecase/`
- Modify: `quote/application/service/QuoteService.java` and split orchestration into focused services.
- Modify: `quote/adapter/in/web/controller/QuoteController.java`, submission imports, tests, and package-info files.

**Interfaces:**

Create focused public ports with matching methods and services:

```java
CreateQuoteUseCase       → CreateQuoteService
UpdateCoverageUseCase    → UpdateCoverageService
GetQuoteUseCase          → GetQuoteService
SearchQuotesUseCase      → SearchQuotesService
GetQuoteSummaryUseCase   → GetQuoteSummaryService
```

Keep the quote lifecycle capabilities required by submission as explicit,
small quote API interfaces rather than reintroducing a broad facade. Preserve
the existing owner-isolation and administrator behavior.

- [ ] **Step 1: Add compile-level/controller contract tests for focused interfaces**
- [ ] **Step 2: Run quote tests and verify they fail because the focused interfaces and target result names do not exist**
- [ ] **Step 3: Rename result/query records and split `QuoteService` into focused services**
- [ ] **Step 4: Update controller and submission callers; assert serialized JSON remains identical in controller tests**
- [ ] **Step 5: Run quote unit/controller tests and expect PASS**
- [ ] **Step 6: Commit**

```bash
git add service/src/main/java/com/clara/insurancequotes/quote service/src/test/java/com/clara/insurancequotes/quote service/src/main/java/com/clara/insurancequotes/submission
git commit -m "refactor(quote): expose focused use cases and result names"
```

## Task 5: Move authentication transport models behind the web adapter

**Files:**

- Rename: `auth/api/request/WebAuthnAssertRequest.java` → `auth/adapter/in/web/request/WebAuthnAssertRequest.java`
- Rename: `auth/api/request/WebAuthnRegisterRequest.java` → `auth/adapter/in/web/request/WebAuthnRegisterRequest.java`
- Create: `auth/adapter/in/web/request/LoginRequest.java`
- Create: `auth/adapter/in/web/request/RefreshRequest.java`
- Create: `auth/adapter/in/web/request/AssertionOptionsRequest.java`
- Modify: `auth/adapter/in/web/controller/AuthController.java`
- Create: focused `auth/api/usecase/*UseCase.java` contracts and matching application service implementations.
- Modify: auth tests and package-info files.

**Interfaces:**

The controller depends only on public use-case interfaces. Request records stay
HTTP-specific and keep the current JSON property names and validation
annotations.

- [ ] **Step 1: Add controller tests proving the existing request JSON maps to the target request classes**
- [ ] **Step 2: Run `mvn -pl service -Dtest=AuthControllerTest test` and verify target imports fail**
- [ ] **Step 3: Extract request records and introduce focused authentication use-case interfaces**
- [ ] **Step 4: Update controller wiring and preserve login, refresh, logout, and WebAuthn responses**
- [ ] **Step 5: Run all auth tests and expect PASS**
- [ ] **Step 6: Commit**

```bash
git add service/src/main/java/com/clara/insurancequotes/auth service/src/test/java/com/clara/insurancequotes/auth
git commit -m "refactor(auth): isolate web requests and expose use cases"
```

## Task 6: Standardize submission contracts and outbound adapter names

**Files:**

- Rename: `submission/api/usecase/SubmissionApi.java` → `SubmitQuoteUseCase.java`
- Rename: `submission/application/service/SubmissionService.java` → `SubmitQuoteService.java`
- Rename: `submission/application/port/out/InsurerGateway.java` → `InsurerSubmissionPort.java`
- Rename: `submission/adapter/out/client/insurer/HttpInsurerClient.java` → `InsurerHttpClient.java`
- Rename: `submission/adapter/out/client/insurer/InsurerClientConfig.java` → `InsurerClientConfiguration.java`
- Rename: `submission/application/service/SubmissionFinalizer.java` → `FinalizeQuoteSubmissionService.java` where the class is an application service.
- Modify: submission controller, tests, package-info files, and quote lifecycle API imports.

- [ ] **Step 1: Update submission tests to target the new interface and adapter names**
- [ ] **Step 2: Run submission tests and verify the target names fail to compile**
- [ ] **Step 3: Apply the renames without changing HTTPBin request/response behavior**
- [ ] **Step 4: Run submission tests and the real insurer-boundary integration test**
- [ ] **Step 5: Commit**

```bash
git add service/src/main/java/com/clara/insurancequotes/submission service/src/test/java/com/clara/insurancequotes/submission
git commit -m "refactor(submission): standardize use-case and client names"
```

## Task 7: Align persistence, client, and mapper naming across modules

**Files:**

- Rename quote persistence implementation to `quote/adapter/out/persistence/QuotePersistenceAdapter.java`.
- Rename auth persistence implementations to explicit `*PersistenceAdapter` names while retaining `SpringData`/`Jpa` in framework-specific repository names.
- Add or update `*PersistenceMapper`, `*WebMapper`, and `*MessageMapper` package placement where current code already performs that translation.
- Update all tests and imports.

- [ ] **Step 1: Add architecture assertions that application ports are implemented only by outbound adapters**
- [ ] **Step 2: Run `mvn -pl service -Dtest=ModularityTest test` and verify the assertions fail for old adapter names/packages**
- [ ] **Step 3: Apply names and package-info documentation without changing persistence queries or mappings**
- [ ] **Step 4: Run persistence/controller tests and expect PASS**
- [ ] **Step 5: Commit**

```bash
git add service/src/main/java service/src/test/java
git commit -m "refactor(adapters): make persistence and client roles explicit"
```

## Task 8: Enforce boundaries and refresh architecture documentation

**Files:**

- Modify: `service/src/test/java/com/clara/insurancequotes/ModularityTest.java`
- Modify: `docs/architecture/modules/*.adoc`
- Modify: `docs/architecture/modules/*.puml`
- Modify: `docs/architecture/modules/all-docs.adoc`
- Modify: `docs/decisions/ADR-001-spring-modulith-package-boundaries.md`
- Modify: `README.md` or the backend architecture section that links to module docs.

- [ ] **Step 1: Add architecture tests for domain/framework independence and transport DTO placement**
- [ ] **Step 2: Run the architecture tests and verify the new rules fail against any remaining violation**
- [ ] **Step 3: Fix remaining package references and regenerate Modulith documentation**
- [ ] **Step 4: Run the complete Maven test suite**

```bash
mvn -pl service test
```

- [ ] **Step 5: Run formatting and static checks**

```bash
mvn -pl service verify
```

- [ ] **Step 6: Commit**

```bash
git add docs service/src/test/java/com/clara/insurancequotes/ModularityTest.java README.md
git commit -m "test(modulith): enforce package and naming boundaries"
```

## Task 9: Full application verification and PR handoff

- [ ] Run unit, integration, and architecture tests using the repository’s
  supported Maven commands.
- [ ] Run the JVM Compose smoke path with the existing demo command.
- [ ] Confirm `/api/actuator/health`, quote creation, coverage update, quote
  search, authentication, and insurer submission still work.
- [ ] Confirm no old package names remain with targeted `rg` searches.
- [ ] Confirm `git diff --check`, no secrets, and a clean worktree.
- [ ] Update `docs/architecture/module-package-structure-design.md` status to
  implemented and mark this plan complete.
- [ ] Push `feat-modulith-package-structure` and create a PR targeting `main`.

## Definition of Done

- [ ] All tasks are complete and committed in logical units.
- [ ] All existing tests pass with zero skipped tests caused by this refactor.
- [ ] Spring Modulith verifies module boundaries.
- [ ] All module package-info files explain the responsibility and visibility.
- [ ] Public contracts use consistent command/query/result/use-case/event/
  exception/type naming.
- [ ] HTTP, persistence, Kafka, Redis, pricing, authentication, and submission
  behavior remain unchanged.
- [ ] The PR description links the design document, this plan, ADR-001, and
  the verification commands/results.
