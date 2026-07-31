# Task 1 Report: Add failing architecture naming guardrails

## Status

Complete. Task 1 added the requested architecture naming guardrails as an intentionally failing test. No production classes, architecture plans, or design documents were modified.

## Commit

- `cb6a1f2` — `test(architecture): add module naming guardrails`

## Test

Command:

```text
mise exec -- mvn -pl service -Dtest=ArchitectureNamingTest test
```

Result: expected red state; Maven exited with status 1.

Surefire reported 5 tests run, 2 failures, and 0 errors:

- `springConfigurationClassesUseConfigurationSuffix` failed for eight current classes: `DemoUserSeeder`, `JwtConfig`, `SecurityConfig`, `WebAuthnConfig`, `ExpirationScheduleConfig`, `CacheConfig`, `ClockConfig`, and `I18nConfig`.
- `corePackagesDoNotDependOnWebOrInfrastructureTypes` failed because `LoginResponse` depends on Jackson annotations.
- `useCasesArePublicContracts` passed.
- `requestsRemainInboundWebAdapterDetails` passed.
- `persistenceAdaptersRemainOutboundPersistenceDetails` passed.

The direct `mvn` command was also attempted first, but this environment has no `mvn` executable on `PATH`; the repository-configured Maven 3.9.9 installation was used through `mise exec`.

## Files

Created:

- `service/src/test/java/com/clara/insurancequotes/ArchitectureNamingTest.java`
- `.superpowers/sdd/task-1-report.md`

Intentionally preserved as pre-existing uncommitted changes:

- `docs/architecture/modules/components.puml`
- `docs/architecture/modules/module-quote.adoc`
- `docs/architecture/modules/module-submission.puml`

## Concerns

The brief predicted the configuration rule as the expected red result from six `*Config` classes. The actual compiled application contains two additional annotated classes with non-`Configuration` suffixes, and the core dependency rule also exposes an existing Jackson dependency. These are recorded as intentional follow-up failures for later tasks; this task does not rename production classes or relax the guardrails.
