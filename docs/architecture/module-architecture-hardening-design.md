# Module architecture hardening design

## Status

Approved for planning

## Scope

Harden the existing backend package architecture across the Spring Modulith
modules `auth`, `pricing`, `quote`, `submission`, and `shared`. The work is a
behavior-neutral consistency pass over package responsibilities, class names,
public module contracts, architecture tests, and documentation.

The existing frontend FSD structure remains unchanged. Its generated API
contract is affected only if a backend wire contract actually changes; this
design does not authorize HTTP or JSON contract changes.

## Goals

- Make module ownership and dependency direction obvious from package names.
- Apply one naming vocabulary to commands, queries, results, use cases, domain
  services, adapters, requests, responses, events, ports, and configuration.
- Keep `api` as the only cross-module contract surface exposed through Spring
  Modulith named interfaces.
- Keep application services dependent on technology-neutral outbound ports.
- Keep HTTP concerns in inbound web adapters and persistence/client concerns in
  outbound adapters.
- Strengthen automated checks so new classes cannot silently reintroduce flat
  packages or inward dependency violations.
- Explain the structure concisely in the architecture documentation and module
  diagrams.

## Non-goals

- No change to HTTP paths, API-versioning headers, request/response JSON,
  OpenAPI schema names, database tables, Flyway migrations, or event payloads.
- No new Maven module per business capability.
- No frontend package migration from Feature-Sliced Design.
- No Kafka, Redis, observability, or authentication feature changes.
- No full persistence purity migration in this pass.

## Architectural decision

Use a responsibility-based package structure inside each business module:

```text
module/
├── api/
│   ├── command/       # immutable state-changing intentions
│   ├── query/         # immutable read requests
│   ├── result/        # stable public answers
│   ├── usecase/       # public inbound-port interfaces
│   ├── event/         # public past-tense facts
│   ├── exception/     # expected public failures
│   └── type/          # small public vocabulary/value types
├── application/
│   ├── service/       # use-case orchestration
│   ├── port/out/      # technology-neutral outbound ports
│   └── mapper/        # application/domain translation
├── domain/
│   ├── model/         # aggregates, entities, value objects, enums
│   ├── service/       # stateless business policies
│   ├── exception/     # invariant and business-rule failures
│   ├── event/         # internal domain facts
│   └── specification/ # reusable business predicates when justified
├── adapter/
│   ├── in/            # web, messaging, and scheduled entry points
│   └── out/           # persistence, clients, messaging, cache, telemetry
└── configuration/     # Spring and module wiring
```

Only packages that contain a real responsibility are created. A
`package-info.java` is required at module roots, public named interfaces, and
meaningful responsibility packages; repetitive empty intermediate packages do
not need one.

## Naming standard

| Responsibility | Naming convention | Example |
| --- | --- | --- |
| Command | `<Verb><Noun>Command` | `CreateQuoteCommand` |
| Query | `Get<Noun>Query` or `Search<Noun>Query` | `SearchQuotesQuery` |
| Public result | `<Noun>Details`, `<Noun>Summary`, `<Noun>Page` | `QuoteDetails` |
| Use case | `<Verb><Noun>UseCase` | `SubmitQuoteUseCase` |
| Application service | `<Verb><Noun>Service` | `SubmitQuoteService` |
| Public/domain event | `<Noun><PastTenseVerb>` | `QuoteSubmitted` |
| API exception | `<Noun>Exception` | `QuoteNotFoundException` |
| Domain exception | `<Rule>Exception` | `IncompleteQuoteException` |
| Domain model | Business noun | `Quote`, `HealthProfile` |
| Domain policy | `<Noun>Policy` or focused business noun | `QuotePricingPolicy` |
| Inbound controller | `<Module>Controller` | `QuoteController` |
| HTTP request/response | `<Verb><Noun>Request` / `<Noun>Response` | `LoginRequest`, `TokenPairResponse` |
| Outbound port | Capability noun without technology | `QuoteRepository`, `PricingPort` |
| Persistence adapter | `<Noun>PersistenceAdapter` | `QuotePersistenceAdapter` |
| Framework repository | `SpringData<Noun>Repository` | `SpringDataQuoteRepository` |
| External client | `<Provider>HttpClient` | `InsurerHttpClient` |
| Mapper | `<Boundary>Mapper` | `QuoteWebMapper` |
| Configuration | `<Module><Concern>Configuration` | `QuoteCacheConfiguration` |

`Request` and `Response` remain transport suffixes by design. They are not
used for domain objects or application results. API versioning remains in the
web adapter and continues to use Spring's `API-Version` header convention.

## Dependency rules

```text
adapter.in.web ───────▶ api.usecase ───────▶ application ───────▶ domain
                                      │
                                      └──────▶ application.port.out

adapter.out.persistence/client/messaging/cache ───────▶ application.port.out
```

- Controllers, consumers, and schedulers invoke use-case interfaces.
- Application services implement use cases and orchestrate domain behavior.
- Domain code does not depend on HTTP, Spring MVC, Kafka, Redis, external HTTP
  clients, or API error abstractions.
- `shared.error.ApiException` remains HTTP-only. Domain exceptions extend a
  transport-neutral runtime exception and are translated by adapter advice.
- Cross-module code imports only named public APIs and never implementation,
  adapter, persistence, or domain packages.
- The current JPA mapping on `quote.domain.model` is retained as an explicit,
  documented transitional exception. A separate migration is required before
  enforcing a completely framework-free domain model.

## Planned implementation slices

1. Audit remaining ambiguous names and package placements against the standard.
2. Apply only behavior-neutral moves/renames that improve responsibility
   clarity; preserve wire names and persistence names.
3. Add focused package-level documentation where it explains a boundary.
4. Extend Modulith and ArchUnit tests for public interfaces, forbidden inward
   dependencies, transport suffix placement, and adapter naming.
5. Update the architecture checklist, class naming map, and module diagrams.
6. Run unit, architecture, and relevant integration verification before review.

## Verification and acceptance criteria

- `ApplicationModules.verify()` passes for all documented modules.
- Architecture tests reject cross-module implementation imports and adapter
  dependencies from API/application/domain packages.
- Every `Request` and `Response` type is located in an inbound web adapter.
- Every `*UseCase` is a public API interface and every corresponding
  application implementation uses the `<Verb><Noun>Service` convention.
- Persistence adapters implement application ports and framework repositories
  remain outbound adapter details.
- Existing unit, integration, REST E2E, OpenAPI, and frontend contract checks
  remain green.
- No generated architecture document overwrites existing user edits.
- The working tree contains no unintentional changes after verification.

## Deferred follow-up

Extract `QuoteEntity` and persistence-specific embeddables from the domain
aggregate, add explicit persistence mappers, and then enforce a framework-free
domain package. This requires a schema-safe migration and is intentionally
tracked separately from naming and boundary hardening.
