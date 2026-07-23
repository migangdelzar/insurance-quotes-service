# Package Structure Design: Responsibility-Based Enterprise Layout

## Status

Approved by the user on 2026-07-22.

## Decision

The backend uses responsibility-based packages. Public contract models live under `api/model`; published inbound interfaces live under `api/port`; domain entities and business invariants live under `domain/model`; domain rule failures live under `domain/exception`; and application lookup/use-case failures live under `application/exception`.

The shared `error/ApiException` remains transport-facing only. Domain exceptions extend a framework-neutral module base such as `QuoteException` and never depend on HTTP status codes, Spring MVC, or `ApiException`.

## Quote layout

```text
com.clara.insurancequotes
├── error
│   └── ApiException.java
└── quote
    ├── api
    │   ├── model
    │   │   └── HealthCondition.java
    │   └── port
    │       └── QuoteApi.java
    ├── application
    │   ├── exception
    │   │   └── QuoteNotFoundException.java
    │   ├── port/out
    │   │   └── QuoteRepository.java
    │   └── service
    ├── domain
    │   ├── exception
    │   │   ├── QuoteException.java
    │   │   ├── InvalidStateTransitionException.java
    │   │   ├── HealthDataNotAllowedException.java
    │   │   └── IncompleteQuoteException.java
    │   └── model
    │       ├── Quote.java
    │       ├── QuoteStatus.java
    │       └── HealthProfile.java
    └── infrastructure
```

Commands and views introduced by later tasks are public contract models and therefore live in `quote/api/model`. Persistence adapters remain under `quote/infrastructure/outbound/persistence`; test fixtures mirror production responsibility under `quote/domain/model`.

## Pricing layout

```text
pricing
├── api
│   ├── model
│   │   ├── CoverageType.java
│   │   ├── PricingInput.java
│   │   └── Premium.java
│   └── port/in
│       └── PremiumCalculator.java
├── application/service
├── domain/policy
└── infrastructure
```

## Test layout

Tests mirror the responsibility of the production class. `QuoteTest` covers aggregate behavior, `HealthProfileTest` covers health-profile behavior, and exception tests exist only when an exception has meaningful code, metadata, message, or mapping behavior. `QuoteMother` lives under `testFixtures/.../quote/domain/model` because it creates domain models.

## Constraints

- No flat mixed `api`, `domain`, `application`, or `exception` packages.
- Domain code must not depend on HTTP-oriented abstractions.
- Package moves are mechanical refactors; behavior and public method signatures remain stable unless the package itself is the requested change.
- All imports, plan paths, examples, and architecture checks must use the same layout.
