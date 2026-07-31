# Module architecture hardening implementation plan

> **For agentic workers:** Execute the tasks in order. Use TDD for each
> behavior or architecture rule, keep commits small, and preserve the three
> pre-existing edits in `docs/architecture/modules/`.

**Goal:** Complete the behavior-neutral naming and boundary hardening pass for
the `auth`, `pricing`, `quote`, `submission`, and `shared` Spring
Modulith modules.

**Architecture:** Keep one Spring Boot deployment with responsibility-based
Spring Modulith modules. Each module exposes only named `api` interfaces,
coordinates use cases in `application`, protects business rules in
`domain`, and hides technical integrations behind `adapter.in` and
`adapter.out`.

**Tech Stack:** Java 17, Spring Boot, Spring Modulith, ArchUnit, JUnit 5,
Maven, Lombok, SLF4J, PostgreSQL/JPA, Redis, Kafka, and the existing REST/E2E
test toolchain.

## Global Constraints

- Do not change HTTP paths, API-versioning headers, request/response JSON,
  OpenAPI schema names, database tables, Flyway migrations, or event payloads.
- Keep `Request` and `Response` suffixes for transport contracts. The existing
  authentication result names `LoginResponse`, `TokenPairResponse`, and
  `WebAuthnChallengeResponse` remain public API compatibility exceptions.
- Do not migrate the frontend from Feature-Sliced Design.
- Do not extract Maven modules or change Kafka, Redis, observability, or
  authentication behavior.
- Keep `shared.error.ApiException` HTTP-only; domain exceptions must remain
  transport-neutral.
- Do not introduce Lombok `@Data` or generated mutable accessors into domain
  aggregates. Use Lombok only where it reduces boilerplate in framework,
  adapter, and application wiring classes.
- Add `@Slf4j` only where a class emits meaningful operational logs; do not add
  logging solely to satisfy a naming rule.
- Do not run `ModularityTest.generateModuleDocumentation` as an isolated
  validation command because it can overwrite the user’s existing diagram
  edits. Update those files deliberately in the documentation task.
- The existing uncommitted edits to
  `docs/architecture/modules/components.puml`,
  `docs/architecture/modules/module-quote.adoc`, and
  `docs/architecture/modules/module-submission.puml` belong to the user and
  must not be reverted.

---

## File map

### Files to create

- `service/src/test/java/com/clara/insurancequotes/ArchitectureNamingTest.java`
  — architecture naming and package-placement rules.
- `service/src/main/java/com/clara/insurancequotes/auth/configuration/DemoUserSeedingConfiguration.java`
  — startup seeding configuration under the correct responsibility package.

### Files to rename or move

| Current path | Target path | Reason |
| --- | --- | --- |
| `auth/configuration/JwtConfig.java` | `auth/configuration/JwtConfiguration.java` | Configuration suffix |
| `auth/configuration/WebAuthnConfig.java` | `auth/configuration/WebAuthnConfiguration.java` | Configuration suffix |
| `auth/configuration/SecurityConfig.java` | `auth/configuration/SecurityConfiguration.java` | Configuration suffix |
| `auth/application/service/DemoUserSeeder.java` | `auth/configuration/DemoUserSeedingConfiguration.java` | It wires an `ApplicationRunner`; it is configuration, not a use-case service |
| `pricing/domain/service/PremiumCalculator.java` | `pricing/domain/service/PremiumCalculationPolicy.java` | Explicit domain-policy role |
| `quote/configuration/CacheConfig.java` | `quote/configuration/QuoteCacheConfiguration.java` | Module and concern are explicit |
| `quote/adapter/in/scheduler/ExpirationScheduleConfig.java` | `quote/adapter/in/scheduler/QuoteExpirationConfiguration.java` | Scheduler configuration is explicit and module-owned |
| `shared/configuration/ClockConfig.java` | `shared/configuration/ClockConfiguration.java` | Configuration suffix |
| `shared/configuration/I18nConfig.java` | `shared/configuration/I18nConfiguration.java` | Configuration suffix |

`shared.error.ApiError` is retained because its shape is referenced by the
requirements and HTTP contract. `ApiException` is retained as the explicit
HTTP-only exception abstraction. `QuotePersistenceAdapter`,
`SpringDataQuoteRepository`, `InsurerHttpClient`, `QuoteApplicationMapper`,
and existing `*UseCase` names already communicate their responsibilities and
do not need cosmetic renames.

### Files to modify

- `service/src/test/java/com/clara/insurancequotes/ModularityTest.java` —
  extend module named-interface and forbidden-dependency assertions.
- `service/src/test/java/com/clara/insurancequotes/adapter/PersistenceAdapterStructureTest.java`
  — retain port-to-adapter assertions and cover provider-specific adapter
  placement where useful.
- `service/src/test/java/com/clara/insurancequotes/auth/AuthPackageStructureTest.java`
  — update renamed configuration/seeding paths.
- `service/src/test/java/com/clara/insurancequotes/auth/application/service/DemoUserSeederTest.java`
  — move package/class ownership to configuration and retain behavior tests.
- `service/src/test/java/com/clara/insurancequotes/pricing/application/service/CalculatePremiumServiceTest.java`
  — construct `PremiumCalculationPolicy`.
- Controller, integration, and application tests importing renamed
  configuration classes — update imports only; assertions and behavior remain
  unchanged.
- `service/src/main/java/com/clara/insurancequotes/pricing/application/service/CalculatePremiumService.java`
  and `pricing/configuration/PricingConfiguration.java` — use the renamed
  policy type and preserve the `CalculatePremiumUseCase` contract.
- Quote application services and the cache listener — use
  `QuoteCacheConfiguration.QUOTES_CACHE`.
- Scheduler configuration and its test references — use
  `QuoteExpirationConfiguration`.
- `docs/architecture/module-package-structure-design.md` — add the final
  naming map and the compatibility exceptions.
- `docs/architecture/modules/*.adoc` and relevant `.puml` files — document
  the final names and preserve the user’s existing relationship edits.
- `docs/decisions/ADR-001-spring-modulith-package-boundaries.md` — link the
  hardening design and clarify that the current JPA-on-domain mapping is
  transitional.
- `README.md` — add a concise architecture checklist link and the module
  naming rule without duplicating the full design document.

---

## Task 1: Add failing architecture naming guardrails

**Purpose:** Capture the target rules before changing names. The test must fail
against the current `*Config` classes, proving it protects the intended
change.

**Files:**

- Create: `service/src/test/java/com/clara/insurancequotes/ArchitectureNamingTest.java`
- Modify: none in production

**Interfaces:**

- Consumes: compiled application classes from `target/classes` and the current
  `ApplicationModules` model.
- Produces: executable rules for configuration suffixes, use-case placement,
  inbound request placement, persistence adapter placement, and forbidden core
  dependencies.

- [ ] **Step 1: Write the failing tests**

Create the test with these focused rules:

```java
package com.clara.insurancequotes;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;

class ArchitectureNamingTest {

    private static final JavaClasses APPLICATION_CLASSES =
            new ClassFileImporter().importPath(Path.of("target", "classes"));

    @Test
    void springConfigurationClassesUseConfigurationSuffix() {
        classes()
                .that()
                .areAnnotatedWith(Configuration.class)
                .should()
                .haveSimpleNameEndingWith("Configuration")
                .check(APPLICATION_CLASSES);
    }

    @Test
    void useCasesArePublicContracts() {
        classes()
                .that()
                .haveSimpleNameEndingWith("UseCase")
                .should()
                .resideInAnyPackage("..api.usecase..")
                .check(APPLICATION_CLASSES);
    }

    @Test
    void requestsRemainInboundWebAdapterDetails() {
        classes()
                .that()
                .haveSimpleNameEndingWith("Request")
                .should()
                .resideInAnyPackage("..adapter.in.web.request..")
                .check(APPLICATION_CLASSES);
    }

    @Test
    void persistenceAdaptersRemainOutboundPersistenceDetails() {
        classes()
                .that()
                .haveSimpleNameEndingWith("PersistenceAdapter")
                .should()
                .resideInAnyPackage("..adapter.out.persistence..")
                .check(APPLICATION_CLASSES);
    }

    @Test
    void corePackagesDoNotDependOnWebOrInfrastructureTypes() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "..api.command..",
                        "..api.query..",
                        "..api.type..",
                        "..api.usecase..",
                        "..api.exception..",
                        "..application..",
                        "..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework.web..",
                        "org.springframework.http..",
                        "org.springframework.kafka..",
                        "org.springframework.data.redis..",
                        "com.fasterxml.jackson..")
                .check(APPLICATION_CLASSES);
    }
}
```

- [ ] **Step 2: Run the new tests and verify the expected red state**

Run:

```bash
mvn -pl service -Dtest=ArchitectureNamingTest test
```

Expected: failure from `springConfigurationClassesUseConfigurationSuffix`
because `JwtConfig`, `SecurityConfig`, `WebAuthnConfig`, `ClockConfig`,
`I18nConfig`, and `CacheConfig` still exist.

- [ ] **Step 3: Commit the failing guardrails**

```bash
git add service/src/test/java/com/clara/insurancequotes/ArchitectureNamingTest.java
git commit -m "test(architecture): add module naming guardrails"
```

---

## Task 2: Standardize Spring configuration names and ownership

**Files:**

- Rename the nine configuration/seeding files listed in the file map.
- Modify all production imports and bean references found by `rg`.
- Move and rename `DemoUserSeederTest.java` to the configuration package and
  class name.
- Modify controller and integration test configuration imports.

**Interfaces:**

- Consumes: existing configuration properties, beans, and application ports.
- Produces: the same Spring beans and bean names, with class names ending in
  `Configuration`.

- [ ] **Step 1: Rename files mechanically**

Run the following from `insurance-quotes-service`:

```bash
git mv service/src/main/java/com/clara/insurancequotes/auth/configuration/JwtConfig.java service/src/main/java/com/clara/insurancequotes/auth/configuration/JwtConfiguration.java
git mv service/src/main/java/com/clara/insurancequotes/auth/configuration/WebAuthnConfig.java service/src/main/java/com/clara/insurancequotes/auth/configuration/WebAuthnConfiguration.java
git mv service/src/main/java/com/clara/insurancequotes/auth/configuration/SecurityConfig.java service/src/main/java/com/clara/insurancequotes/auth/configuration/SecurityConfiguration.java
git mv service/src/main/java/com/clara/insurancequotes/auth/application/service/DemoUserSeeder.java service/src/main/java/com/clara/insurancequotes/auth/configuration/DemoUserSeedingConfiguration.java
git mv service/src/main/java/com/clara/insurancequotes/pricing/domain/service/PremiumCalculator.java service/src/main/java/com/clara/insurancequotes/pricing/domain/service/PremiumCalculationPolicy.java
git mv service/src/main/java/com/clara/insurancequotes/quote/configuration/CacheConfig.java service/src/main/java/com/clara/insurancequotes/quote/configuration/QuoteCacheConfiguration.java
git mv service/src/main/java/com/clara/insurancequotes/quote/adapter/in/scheduler/ExpirationScheduleConfig.java service/src/main/java/com/clara/insurancequotes/quote/adapter/in/scheduler/QuoteExpirationConfiguration.java
git mv service/src/main/java/com/clara/insurancequotes/shared/configuration/ClockConfig.java service/src/main/java/com/clara/insurancequotes/shared/configuration/ClockConfiguration.java
git mv service/src/main/java/com/clara/insurancequotes/shared/configuration/I18nConfig.java service/src/main/java/com/clara/insurancequotes/shared/configuration/I18nConfiguration.java
```

- [ ] **Step 2: Update declarations and references**

Use exact replacements, then inspect the diff:

```text
JwtConfig                    → JwtConfiguration
WebAuthnConfig               → WebAuthnConfiguration
SecurityConfig               → SecurityConfiguration
DemoUserSeeder               → DemoUserSeedingConfiguration
PremiumCalculator            → PremiumCalculationPolicy
CacheConfig                  → QuoteCacheConfiguration
ExpirationScheduleConfig     → QuoteExpirationConfiguration
ClockConfig                  → ClockConfiguration
I18nConfig                   → I18nConfiguration
```

Keep existing explicit bean names such as `openApiConfig` and
`webVersioningConfig` because tests and runtime configuration use them; class
names and bean names are separate concerns.

- [ ] **Step 3: Update the seeding test package and class name**

Move the test to:

```text
service/src/test/java/com/clara/insurancequotes/auth/configuration/
    DemoUserSeedingConfigurationTest.java
```

Keep its existing assertions for idempotent user creation and password
encoding. Change only the package, class name, and constructor/reference.

- [ ] **Step 4: Run focused tests**

```bash
mvn -pl service -Dtest=ArchitectureNamingTest,AuthPackageStructureTest,DemoUserSeedingConfigurationTest,CalculatePremiumServiceTest test
```

Expected: PASS, including the new configuration suffix rule and all existing
seeding/pricing behavior.

- [ ] **Step 5: Commit the naming slice**

```bash
git add service/src/main/java service/src/test/java
git commit -m "refactor(architecture): standardize configuration names"
```

---

## Task 3: Harden public API and adapter boundary tests

**Files:**

- Modify: `service/src/test/java/com/clara/insurancequotes/ModularityTest.java`
- Modify: `service/src/test/java/com/clara/insurancequotes/adapter/PersistenceAdapterStructureTest.java`
- Modify: relevant module `package-info.java` files only when the test exposes
  a documented named interface.

**Interfaces:**

- Consumes: `ApplicationModules.of(Application.class)` and compiled classes.
- Produces: executable proof that `auth`, `pricing`, `quote`,
  `submission`, and `shared` expose only intended API packages.

- [ ] **Step 1: Add regression assertions for named interfaces**

Extend `ModularityTest` with checks for the existing public packages:

```java
@Test
void exposesPricingUseCaseAndResultContracts() {
    var pricing = MODULES.getModuleByName("pricing").orElseThrow();

    assertThat(pricing.getNamedInterfaces().getByName("pricing-api-command")).isPresent();
    assertThat(pricing.getNamedInterfaces().getByName("pricing-api-result")).isPresent();
    assertThat(pricing.getNamedInterfaces().getByName("pricing-api-type")).isPresent();
    assertThat(pricing.getNamedInterfaces().getByName("pricing-api-usecase")).isPresent();
}

@Test
void exposesSubmissionUseCaseEventAndExceptionContracts() {
    var submission = MODULES.getModuleByName("submission").orElseThrow();

    assertThat(submission.getNamedInterfaces().getByName("submission-api-usecase"))
            .isPresent();
    assertThat(submission.getNamedInterfaces().getByName("submission-api-event"))
            .isPresent();
    assertThat(submission.getNamedInterfaces().getByName("submission-api-exception"))
            .isPresent();
}
```

These assertions should pass with the current package-info annotations. If any
assertion fails, add the corresponding `@NamedInterface` annotation to that
package’s existing `package-info.java` before continuing. Do not expose
application, domain, or adapter packages.

- [ ] **Step 2: Add forbidden dependency assertions**

Add rules proving API packages do not depend on application, domain, or
adapter implementations, and proving domain/application code does not depend
on `shared.error` or `org.springframework.http`:

```java
@Test
void publicApisDoNotDependOnImplementations() {
    noClasses()
            .that()
            .resideInAnyPackage("..api..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..application..", "..domain..", "..adapter..")
            .check(APPLICATION_CLASSES);
}

@Test
void domainAndApplicationDoNotDependOnHttpErrorAbstractions() {
    noClasses()
            .that()
            .resideInAnyPackage("..domain..", "..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..shared.error..",
                    "org.springframework.http..",
                    "org.springframework.web..")
            .check(APPLICATION_CLASSES);
}
```

The current JPA annotations on `quote.domain.model` are deliberately not
rejected in this task; the design document records that dependency as a
separate migration.

- [ ] **Step 3: Run architecture tests**

```bash
mvn -pl service -Dtest=ModularityTest,ArchitectureNamingTest,PersistenceAdapterStructureTest test
```

Expected: PASS with all module contracts discovered and no forbidden
dependencies.

- [ ] **Step 4: Commit the boundary guardrails**

```bash
git add service/src/main/java service/src/test/java
git commit -m "test(architecture): enforce public module boundaries"
```

---

## Task 4: Make package documentation and naming references consistent

**Files:**

- Modify: `docs/architecture/module-package-structure-design.md`
- Modify: `docs/architecture/modules/module-auth.adoc`
- Modify: `docs/architecture/modules/module-pricing.adoc`
- Modify: `docs/architecture/modules/module-quote.adoc`
- Modify: `docs/architecture/modules/module-submission.adoc`
- Modify: `docs/architecture/modules/module-shared.adoc`
- Modify: relevant `.puml` files only where a renamed class is displayed.
- Modify: `docs/decisions/ADR-001-spring-modulith-package-boundaries.md`
- Modify: `README.md`

**Interfaces:**

- Consumes: the approved design document and final Java package/class names.
- Produces: one concise architecture entry point plus detailed module
  references with no stale names.

- [ ] **Step 1: Update the class naming map**

Add the final rename mapping to
`docs/architecture/module-package-structure-design.md`:

```text
JwtConfig                 → JwtConfiguration
WebAuthnConfig            → WebAuthnConfiguration
SecurityConfig            → SecurityConfiguration
DemoUserSeeder            → DemoUserSeedingConfiguration
PremiumCalculator         → PremiumCalculationPolicy
CacheConfig               → QuoteCacheConfiguration
ExpirationScheduleConfig  → QuoteExpirationConfiguration
ClockConfig               → ClockConfiguration
I18nConfig                → I18nConfiguration
```

Document `ApiError`, authentication `*Response` result types, and the
JPA-on-domain transitional exception explicitly.

- [ ] **Step 2: Update module docs without regenerating them**

Replace stale class references manually. Preserve the user’s relationship
ordering and removals in `components.puml`, `module-quote.adoc`, and
`module-submission.puml`; do not run the documenter just to regenerate them.

- [ ] **Step 3: Add the concise README entry point**

Add a short “Backend module architecture” section linking to:

```text
docs/architecture/module-package-structure-design.md
docs/architecture/module-architecture-hardening-design.md
docs/architecture/module-architecture-hardening-plan.md
```

State the four dependency rules in one compact table: `api` is public,
`application` orchestrates, `domain` protects business rules, and
`adapter` contains technical entry points/integrations.

- [ ] **Step 4: Verify no stale names remain in active docs**

Run:

```bash
rg -n "JwtConfig|WebAuthnConfig|SecurityConfig|DemoUserSeeder|PremiumCalculator|CacheConfig|ExpirationScheduleConfig|ClockConfig|I18nConfig" docs README.md service/src/main service/src/test service/src/integrationTest
```

Expected: no matches except historical migration references explicitly labeled
as old names in the naming map.

- [ ] **Step 5: Commit the documentation slice**

```bash
git add docs README.md
git commit -m "docs(architecture): align module naming guidance"
```

---

## Task 5: Run the complete backend verification matrix

**Files:**

- Modify: `docs/architecture/module-architecture-hardening-plan.md` to mark
  completed tasks and record verification results.
- Do not modify source files in this task unless a test exposes a regression
  caused by the approved renames.

- [ ] **Step 1: Run formatting and static checks**

```bash
mvn -pl service -DskipTests compile
mvn -pl service -DskipTests spotless:check
```

Expected: successful compilation and formatting checks.

- [ ] **Step 2: Run unit and architecture tests**

```bash
mvn -pl service test
```
