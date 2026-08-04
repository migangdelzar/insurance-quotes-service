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

## Fix: Scope infrastructure guardrail

The infrastructure dependency rule in `ArchitectureNamingTest` was narrowed
from the broad `..api..` package selector to the plan-specified packages:
`..api.command..`, `..api.query..`, `..api.type..`, `..api.usecase..`,
`..api.exception..`, `..application..`, and `..domain..`. The `api.result`
package is therefore excluded, preserving the documented Jackson-annotated
compatibility exceptions `LoginResponse`, `TokenPairResponse`, and
`WebAuthnChallengeResponse`. No production code or user-owned diagram edits
were changed.

## Fix Test Evidence

Command:

```text
mise exec -- mvn -pl service -Dtest=ArchitectureNamingTest test
```

## Review Correction: Align Plan With Implementation

The Task 1 plan snippet now matches `ArchitectureNamingTest.java`: the
guarded package list includes `..api.result..`, followed by the three exact
compatibility exclusions:

- `com.clara.insurancequotes.auth.api.result.LoginResponse`
- `com.clara.insurancequotes.auth.api.result.TokenPairResponse`
- `com.clara.insurancequotes.auth.api.result.WebAuthnChallengeResponse`

Verification note: `rg -n '\.\.api\.result|LoginResponse|TokenPairResponse|WebAuthnChallengeResponse'`
was run against the plan and test implementation; both contain the same
guarded package and all three exact FQNs.

Result: expected red state; Maven exited with status 1.

Surefire reported 5 tests run, 1 failure, and 0 errors. The sole failure was
`springConfigurationClassesUseConfigurationSuffix`, with the eight intentional
configuration-suffix violations listed above. The infrastructure dependency
rule passed, so the prior Jackson failure from `auth.api.result.LoginResponse`
is gone. The other three naming and placement rules also passed.

## Fix: Restrict response compatibility exception

The infrastructure dependency guardrail again includes `..api.result..`.
It now excludes only these fully qualified compatibility contracts using
ArchUnit's `doNotHaveFullyQualifiedName` predicate:

- `com.clara.insurancequotes.auth.api.result.LoginResponse`
- `com.clara.insurancequotes.auth.api.result.TokenPairResponse`
- `com.clara.insurancequotes.auth.api.result.WebAuthnChallengeResponse`

This preserves enforcement for every other API result class. The identical
class-level predicate chain is documented in the Task 1 plan snippet. No
production classes or user-owned diagram edits were modified.

## Fix Test Evidence: Restricted response compatibility exception

Red verification first restored `..api.result..` without the compatibility
predicates. The targeted test then reported the expected infrastructure
violation from `LoginResponse` (in addition to the intentional configuration
suffix failure), demonstrating that API results are guarded.

After the three exact predicates were added, the command below returned the
expected intentional red state: only
`springConfigurationClassesUseConfigurationSuffix` failed. The infrastructure
dependency rule and the three remaining naming/placement rules passed.
Surefire reported 5 tests run, 1 failure, 0 errors, and 0 skipped; Maven exited
with status 1 solely because Task 1 intentionally retains the eight
configuration-suffix violations.

```text
mise exec -- mvn -pl service -Dtest=ArchitectureNamingTest test
```
