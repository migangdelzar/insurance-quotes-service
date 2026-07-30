# Module package structure design

## Status

Implemented on `feat-modulith-package-structure`

## Goal

Make every Spring Modulith module express the same DDD + hexagonal architecture
through its package names, public API, and package-level documentation. The
refactor is behavior-neutral: HTTP paths, JSON fields, persistence schema,
business rules, and runtime configuration remain unchanged.

The current persistence model keeps JPA annotations on the domain aggregate
classes to preserve the existing schema and migration behavior. The enforced
boundary is that application and domain code does not reach persistence
repositories, adapters, transport DTOs, or infrastructure clients. Splitting
JPA entities from pure domain objects would be a separate schema-safe migration,
not a naming/package refactor.

## Guiding structure

Each business module uses only the packages it actually needs:

```text
module/
├── api/
│   ├── command/       # public state-changing intentions
│   ├── query/         # public read requests
│   ├── result/        # public read/use-case results
│   ├── usecase/       # public inbound-port interfaces
│   ├── event/         # public facts other modules may consume
│   ├── exception/     # expected failures callers may handle
│   └── type/          # small stable public vocabulary/value types
├── application/
│   ├── service/       # use-case orchestration
│   ├── port/out/      # capabilities required from outside
│   └── mapper/        # API/application ↔ domain translation
├── domain/
│   ├── model/         # aggregates, entities, value objects, enums
│   ├── service/       # business behavior spanning domain objects
│   ├── exception/     # invariant and business-rule violations
│   ├── event/         # module-internal domain facts
│   └── specification/ # reusable business predicates
├── adapter/
│   ├── in/            # HTTP, messaging, scheduler entry points
│   └── out/           # persistence, clients, messaging, observability
└── configuration/     # Spring wiring and module configuration
```

An empty package is not created for symmetry. `package-info.java` is added at
module roots, public named-interface boundaries, and meaningful responsibility
packages where documentation or package annotations add value. Repetitive
intermediate package-info files are intentionally omitted.

## Package vocabulary

| Package | Question it answers | Allowed contents |
| --- | --- | --- |
| `api.command` | What state-changing intention can a caller send? | Immutable command records; no business logic |
| `api.query` | What information can a caller request? | Immutable read-query records and query-specific types |
| `api.result` | What public answer does the module return? | Stable read/use-case models, never JPA entities or aggregates |
| `api.usecase` | What capabilities does the module expose? | Focused inbound-port interfaces implemented by application services |
| `api.event` | What fact does the module announce? | Past-tense public events for other modules |
| `api.exception` | What expected failure can callers handle? | Stable module-contract exceptions only |
| `api.type` | What stable vocabulary does the public API share? | Small immutable value types and API enums |
| `application.service` | How is one use case coordinated? | Transactions, orchestration, domain invocation, port calls |
| `application.port.out` | What external capability does the application require? | Technology-neutral outbound interfaces |
| `application.mapper` | How are API and domain models translated? | Application boundary mapping only |
| `domain.model` | What business state and behavior exist? | Aggregates, entities, value objects, domain enums |
| `domain.service` | What business rule spans multiple domain objects? | Stateless domain policies and calculations |
| `domain.exception` | Which business rule or invariant was violated? | Transport-neutral domain exceptions |
| `domain.event` | Which internal domain fact occurred? | Events that do not form another module's contract |
| `domain.specification` | Does an object satisfy a reusable rule? | Composable business predicates; not trivial `if` wrappers |
| `adapter.in` | What external mechanism invokes the application? | Controllers, message consumers, schedulers, transport DTOs |
| `adapter.out` | What technical implementation satisfies a port? | Persistence, HTTP clients, brokers, cache, metrics adapters |
| `configuration` | How are implementations wired? | Spring beans and module/framework configuration only |

The most important vocabulary is:

```text
Command  = please do this
Query    = please tell me this
Result   = here is the answer
Use case = this capability is available
Event    = this already happened
Exception = this operation failed
Type     = this concept has stable public meaning
Service  = these steps coordinate or perform behavior
```

## Class naming conventions

Names must communicate both the business capability and the architectural
role. A class should not need to be opened to discover whether it is a public
contract, an application implementation, or a technical adapter.

| Role | Naming pattern | Example | Naming rule |
| --- | --- | --- | --- |
| Command | `<Verb><Noun>Command` | `CreateQuoteCommand` | Imperative intention; immutable input only |
| Query | `Get<Noun>Query` or `Search<Noun>Query` | `SearchQuotesQuery` | Read-only request; never mutate state |
| Result | `<Noun>Details`, `<Noun>Summary`, `<Noun>Page`, or `<Noun>Result` | `QuoteDetails` | Public answer; do not use persistence or transport suffixes |
| Use case | `<Verb><Noun>UseCase` | `SubmitQuoteUseCase` | Public inbound-port interface; one cohesive capability |
| Application service | `<Verb><Noun>Service` | `SubmitQuoteService` | Implements one use case and coordinates the workflow |
| Public event | `<Noun><PastTenseVerb>` | `QuoteSubmitted` | A fact that already happened; never an imperative |
| Domain event | `<Noun><PastTenseVerb>` | `QuoteExpired` | Internal fact; publish a separate API event when required |
| API exception | `<Noun>Exception` | `QuoteNotFoundException` | Stable expected failure visible to callers |
| Domain exception | `<Rule>Exception` | `IncompleteQuoteException` | Business-rule or invariant violation, transport-neutral |
| Domain aggregate/entity | Business noun | `Quote`, `User` | Owns identity, state, and invariants |
| Value object | Business noun or semantic suffix | `Premium`, `QuoteId` | Immutable and defined by its value |
| Domain policy/service | `<Noun><Policy>` or `<Noun><Service>` | `QuotePricingPolicy` | Stateless business behavior spanning objects |
| Inbound controller | `<Module>Controller` | `QuoteController` | Translates HTTP to a use case; no business logic |
| Inbound request | `<Verb><Noun>Request` | `UpdateCoverageRequest` | HTTP wire model only; lives under the web adapter |
| Inbound message consumer | `<Event>Consumer` | `CustomerUpdatedConsumer` | Translates a broker message to an application operation |
| Outbound port | Capability noun with no technology name | `PricingPort` | Technology-neutral interface required by application |
| Persistence port | `<Noun>Repository` | `QuoteRepository` | Application-facing repository interface |
| Persistence adapter | `<Noun>PersistenceAdapter` | `QuotePersistenceAdapter` | Implements a persistence port |
| Spring Data repository | `SpringData<Noun>Repository` | `SpringDataQuoteRepository` | Framework-specific database repository |
| HTTP client | `<Provider>HttpClient` | `InsurerHttpClient` | Performs only the external HTTP call |
| Client adapter | `<Provider><Capability>Adapter` | `InsurerSubmissionAdapter` | Implements the application port around the client |
| External DTO | `<Provider><Request or Response>` | `PricingResponse` | External contract; never leaks into the domain |
| Mapper | `<Boundary>Mapper` | `QuoteWebMapper` | Translates one boundary; do not create generic `Mapper` |
| Configuration | `<Module><Concern>Configuration` | `QuoteCacheConfiguration` | Spring/framework wiring only |
| Exception advice | `<Module>ExceptionHandler` | `QuoteExceptionHandler` | Maps failures to Problem Details/HTTP responses |
| Metrics adapter | `<Module>MetricsAdapter` | `QuoteMetricsAdapter` | Emits telemetry; never makes business decisions |

### Naming rules that prevent ambiguity

- Use `UseCase`, not `Api`, for an inbound contract. `Api` is too broad to
  reveal the capability and encourages a growing facade.
- Use `Request` and `Response` only for transport models. Application results
  use `Details`, `Summary`, `Page`, or `Result`; domain objects do not use
  `Request`, `Response`, or `Dto`.
- Use `View` only when the type is deliberately a read projection. New public
  results should prefer a business name such as `QuoteDetails` or
  `QuoteSummary`.
- Put the technology in adapter names (`SpringData`, `Jpa`, `Http`, `Redis`,
  `Kafka`), never in domain or application-port names.
- Use `Publisher` for an application outbound port and `KafkaPublisher` or
  `SpringQuoteEventPublisher` for its implementation.
- Use past tense for events (`QuoteSubmitted`) and imperative wording for
  commands (`SubmitQuoteCommand`).
- Avoid generic names such as `Manager`, `Helper`, `Util`, `Handler`, and
  `Processor` unless the surrounding package makes the exact responsibility
  unambiguous. `ExceptionHandler` is the intentional HTTP-advice exception.

## HTTP API versioning and transport contracts

The service uses Spring MVC's native API-versioning support with the
`API-Version` request header. Controllers declare the supported mapping version
with `@RequestMapping(..., version = "1.0")` or the equivalent method-level
mapping, while `shared.configuration.WebVersioningConfiguration` resolves and
validates the request version.

Header versioning is the selected convention for this application because it
keeps resource URLs stable, avoids duplicating every route under `/v1`, and
allows Spring to select a newer mapping without changing the resource identity.
The gateway/BFF must forward the header and any cache that varies by API version
must include `API-Version` in its `Vary` key. Do not mix header and path
versioning for the same endpoint family.

Path-segment versioning is also supported by Spring, but adopting it here would
be a public route change requiring coordinated frontend, BFF, OpenAPI, and E2E
updates. It remains a future migration option, not a second active convention.

Versioning belongs to the inbound web adapter, not to the module's application
commands, queries, use cases, domain objects, or events. Those contracts should
remain version-neutral whenever their business meaning is unchanged:

```text
HTTP request (versioned transport DTO)
        ↓
adapter.in.web.mapper
        ↓
api.command / api.query
        ↓
api.usecase
        ↓
api.result
        ↓
HTTP response (versioned transport DTO, when needed)
```

For a compatible version, reuse the same request/response model. When a wire
shape must diverge, keep the `Request`/`Response` suffix and isolate the new
contract under a versioned web-adapter package, for example
`adapter.in.web.v2.request.CreateQuoteRequest` and
`adapter.in.web.v2.response.QuoteResponse`. Both versions may delegate to the
same use case through separate mappers; version-specific branching must not
leak into the domain.

### OpenAPI and enums

The generated OpenAPI contract is authoritative for the HTTP adapter. Public
enums used by request or response models, such as `CoverageType`,
`HealthCondition`, and `QuoteStatusView`, are intentionally emitted as
`type: string` schemas with their allowed `enum` values. This keeps the valid
wire vocabulary visible to Swagger UI and generated clients.

Enums may be repeated inline by the generator; that still preserves the
contract. Extracting them into reusable `components/schemas` with `$ref` is
optional and should only be introduced when a contract-generation or client
reuse requirement justifies the additional customization. Internal domain
enums must not be exposed directly when their persistence or business values
differ from the public API vocabulary.

Authentication transport results intentionally retain the `Response` suffix:
`LoginResponse`, `TokenPairResponse`, and `WebAuthnChallengeResponse`. This
describes their wire role and is consistent with the version-neutral module API
rule above.

### Current-to-target naming map

The following names are part of this refactor. They change Java ownership and
clarity, not the HTTP or persistence contract:

The names in the left column are intentional historical identifiers used to
explain the migration; they are not references to classes that should still
exist after implementation.

| Current name | Target name or names | Reason |
| --- | --- | --- |
| `quote.api.usecase.QuoteApi` | `CreateQuoteUseCase`, `UpdateCoverageUseCase`, `GetQuoteUseCase`, `SearchQuotesUseCase`, `GetQuoteSummaryUseCase`, and focused submission-state use cases | Replaces a broad facade with capability-sized public ports |
| `quote.api.query.QuoteQuery` | `SearchQuotesQuery` | Identifies the query intent |
| `quote.api.result.QuoteView` | `QuoteDetails` | Removes transport/projection ambiguity |
| `quote.api.result.QuotePageView` | `QuotePage` | Names the public page result |
| `quote.api.result.QuoteSummaryView` | `QuoteSummary` | Names the public summary result |
| `quote.api.result.QuoteDistributionView` | `QuoteDistribution` | Names the business read model |
| `quote.api.result.QuoteTrendPointView` | `QuoteTrendPoint` | Names the business read model |
| `quote.application.exception.QuoteNotFoundException` | `quote.api.exception.QuoteNotFoundException` | A stable expected failure belongs to the public contract |
| `quote.application.service.QuoteService` | Focused `<Verb>QuoteService` classes | Aligns one application service with one use case |
| `quote.adapter.out.persistence.JpaQuoteRepository` | `QuotePersistenceAdapter` | Distinguishes the port implementation from Spring Data |
| `auth.adapter.out.persistence.JpaUserRepository` | `SpringDataUserRepository` + `UserPersistenceAdapter` | Separates the Spring Data repository from the application-port adapter |
| `auth.adapter.out.persistence.JpaRefreshTokenRepository` | `SpringDataRefreshTokenRepository` + `RefreshTokenPersistenceAdapter` | Keeps token persistence technology-specific behind the port |
| `auth.adapter.out.persistence.JpaPasskeyCredentialRepository` | `SpringDataPasskeyCredentialRepository` + `PasskeyCredentialPersistenceAdapter` | Keeps passkey persistence technology-specific behind the port |
| `auth.api.request.*` | `auth.adapter.in.web.request.*` | HTTP DTOs are inbound adapter details |
| controller-nested `LoginRequest`, `RefreshRequest`, `AssertionOptionsRequest` | `auth.adapter.in.web.request.*Request` | Keeps transport models out of controllers and API contracts |
| concrete auth services injected into `AuthController` | focused `auth.api.usecase.*UseCase` interfaces | Controllers depend on public capabilities, not implementations |
| `submission.api.usecase.SubmissionApi` | `SubmitQuoteUseCase` | Uses the standard use-case suffix and business verb |
| `submission.adapter.out.client.insurer.HttpInsurerClient` | `InsurerHttpClient` | Provider first, technology second |
| `submission.adapter.out.client.insurer.InsurerClientConfig` | `InsurerClientConfiguration` | Configuration names describe their Spring wiring responsibility |
| `submission.application.service.SubmissionFinalizer` | `FinalizeQuoteSubmissionService` | Application services use a verb-plus-capability name |
| `config.BusinessMetrics` | `shared.observability.BusinessMetrics` | Metrics are cross-cutting observability, not a business module |
| `config.CorrelationIdFilter` | `shared.adapter.in.web.filter.CorrelationIdFilter` | A servlet entry adapter, not generic configuration |
| `config.OpenApiConfig` | `shared.configuration.OpenApiConfiguration` | Consistent configuration suffix |
| `config.WebVersioningConfig` | `shared.configuration.WebVersioningConfiguration` | Consistent configuration suffix |

Existing names that already communicate their role, such as `QuoteRepository`,
`QuoteExpired`, and domain exception names, are retained unless a package move
is required. Names such as `PremiumCalculator` and `DefaultPremiumCalculator`
are renamed because their interface/implementation roles are clearer as
`CalculatePremiumUseCase` and `CalculatePremiumService`. The former
`InsurerGateway` is now the technology-neutral `InsurerSubmissionPort`.

### Consistency across modules

The same role always uses the same suffix and the same interface/implementation
relationship:

```text
<Verb><Noun>Command
<Verb><Noun>UseCase  ← implemented by →  <Verb><Noun>Service
<Noun>Details | <Noun>Summary | <Noun>Page
<Noun><PastTenseVerb>
<Noun>Exception
<Technology><Noun>Repository | <Provider>HttpClient | <Noun>PersistenceAdapter
```

Examples applied consistently to each module:

| Module | Public contract | Application implementation | Technical edge |
| --- | --- | --- | --- |
| `auth` | `LoginUseCase`, `RefreshTokenUseCase`, `AssertPasskeyUseCase` | `LoginService`, `RefreshAccessTokenService`, `WebAuthnService` | `AuthController`, `TokenService`, `UserPersistenceAdapter`, `YubicoPasskeyAdapter` |
| `pricing` | `CalculatePremiumUseCase`, `CalculatePremiumCommand`, `Premium` | `CalculatePremiumService` | `PricingMetricsAdapter` if metrics are module-specific |
| `quote` | `CreateQuoteUseCase`, `UpdateCoverageUseCase`, `SearchQuotesUseCase` | `CreateQuoteService`, `UpdateCoverageService`, `SearchQuotesService` | `QuoteController`, `QuotePersistenceAdapter`, `SpringDataQuoteRepository` |
| `submission` | `SubmitQuoteUseCase`, `QuoteSubmitted`, `InsurerSubmissionPort` | `SubmitQuoteService`, `FinalizeQuoteSubmissionService` | `InsurerHttpClient`, `InsurerSubmissionAdapter` |
| `shared` | `RateLimiter`, `PageQuery`, `PageResult` | `RedisRateLimiter` | `RedisCacheErrorHandler`, `CorrelationIdFilter`, configuration classes |

The matrix is a naming contract, not a requirement to invent classes for
capabilities that do not exist. For example, `PricingMetricsAdapter` is only
created if pricing emits its own metrics; cross-cutting metrics stay in
`shared.observability`.

Interfaces and services use the same verb and noun so that a dependency can be
read without opening the implementation:

```java
public interface SubmitQuoteUseCase {
    QuoteDetails submit(SubmitQuoteCommand command);
}

@Service
final class SubmitQuoteService implements SubmitQuoteUseCase {
    // orchestration only
}
```

The implementation plan must apply this rule to every renamed class and update
all imports, tests, generated Modulith documentation, and package-info files in
the same change. A rename is not complete if an old suffix such as `Api`,
`View`, `Config`, or `Dto` remains for the same responsibility in another
module.

## Module-specific application

### Quote

`quote.api` becomes the public contract for quote creation, coverage updates,
quote retrieval/search, summaries, and quote lifecycle collaboration required
by submission. The broad `QuoteApi` facade is replaced with focused use-case
interfaces so callers depend only on the capability they need.

Read models are named by their purpose (`QuoteDetails`, `QuotePage`,
`QuoteSummary`, `QuoteDistribution`, and `QuoteTrendPoint`). Existing JSON
component names remain unchanged.

`QuoteNotFoundException` is a public application failure and moves to
`quote.api.exception`. Domain violations remain under `quote.domain.exception`.
Transport-only invalid query parsing remains inside the web adapter.

### Authentication

Authentication request records are HTTP wire models, not module API types. They
move from `auth.api.request` to `auth.adapter.in.web.request`. Authentication
operations are exposed through focused `auth.api.usecase` interfaces, while
token and WebAuthn results remain stable public result models with `Response`
suffixes because they are the JSON-facing authentication contract.

The move changes Java ownership only; `/auth/**` routes and JSON fields do not
change.

### Pricing

Pricing already follows most of the convention. Its calculation input remains
a public command, its premium remains a public result, and its calculator is an
inbound use case. Missing package documentation is added to explain the
factor-based domain policy and application implementation.

### Submission

Submission keeps its public event, expected external failures, and focused
submit use case. The HTTP insurer integration remains an outbound client
adapter; the application layer depends only on `InsurerSubmissionPort`.

### Shared and configuration

Cross-cutting code is not treated as a business module. The standalone
`config` module is removed by placing each class with its responsibility:

- metrics → `shared.observability`;
- OpenAPI, versioning, clock, and i18n → `shared.configuration`;
- correlation-id filtering → `shared.adapter.in.web.filter`;
- existing error, cache, and rate-limit code stays under the corresponding
  `shared` responsibility package.

This keeps configuration and observability from appearing to be business
domains while preserving the existing Spring beans.

## Dependency rules

```text
adapter.in  → api.usecase → application → domain
application → application.port.out
adapter.out → application.port.out
```

Other modules may import only explicitly named API packages or public shared
contracts. They must not import another module's `domain`, `application`, or
`adapter` package. The quote module may use the pricing calculator API, and the
submission module may use quote lifecycle APIs, matching the existing business
collaboration.

Domain code must not depend on Spring MVC, JPA, Kafka, Redis, HTTP clients, or
JSON. HTTP request/response models must not be used by application services.

## Spring Modulith visibility

Each top-level module gets a documented `package-info.java` with its display
name. Public API subpackages use named interfaces such as
`quote-api-command`, `quote-api-usecase`, and `quote-api-event`. Internal
packages are not named interfaces and therefore remain implementation details.

`ModularityTest` remains the executable boundary check and is extended with
architecture assertions for domain/framework independence and transport DTO
placement.

## Verification strategy

- Compile and run all existing Maven unit tests.
- Run `ModularityTest` and regenerate the Spring Modulith module documentation.
- Add focused tests for renamed use-case interfaces and moved web request DTOs.
- Verify that the public HTTP contract remains unchanged with controller tests.
- Run the existing Compose smoke path only after the structural tests pass.

## Non-goals

- No endpoint, JSON, database, Kafka, Redis, pricing, authentication, or
  submission behavior changes.
- No new Maven module per business capability.
- No empty `api`, `domain`, or adapter folders solely to match a diagram.
- No HTTP-oriented base exception is introduced into the domain.
