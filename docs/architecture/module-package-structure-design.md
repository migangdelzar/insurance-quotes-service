# Module package structure design

## Status

Approved for implementation

## Goal

Make every Spring Modulith module express the same DDD + hexagonal architecture
through its package names, public API, and package-level documentation. The
refactor is behavior-neutral: HTTP paths, JSON fields, persistence schema,
business rules, and runtime configuration remain unchanged.

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

An empty package is not created for symmetry. `package-info.java` is added to
every package that exists and explains its responsibility in the source tree.

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
token and WebAuthn results remain stable public result models.

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
adapter; the application layer depends only on `InsurerGateway`.

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
