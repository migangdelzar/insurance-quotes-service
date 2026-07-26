# Lessons

## 2026-07-23 — package responsibility correction

- Failure mode: treating every exception as an API exception made the package structure imply that application and domain failures belonged to the transport layer.
- Detection signal: `QuoteNotFoundException` represented a use-case lookup failure, while `ApiException` represented HTTP concerns.
- Prevention rule: classify exceptions by responsibility first; keep domain/application exceptions framework-neutral and translate them only at inbound adapter boundaries.

## 2026-07-23 — framework-native API versioning

- Failure mode: adding `/api/v1` directly to controller paths before checking whether the selected Spring Framework version supports declarative API version conditions.
- Detection signal: Spring Framework 6.2.8's `@RequestMapping` has no `version` attribute; native API versioning requires Spring Framework 7 and Spring Boot 4.
- Prevention rule: verify framework feature availability against the resolved dependency versions before implementing an adapter-level convention; use Spring's `WebMvcConfigurer.configureApiVersioning` and version-aware mappings when the native feature is a requirement.

## 2026-07-26 — telemetry container readiness

- Failure mode: using `wget`-based health checks for Loki and Tempo images that do not ship with `wget`, and gating startup on a short-lived readiness warm-up.
- Detection signal: containers were running and serving logs, but Compose kept them in `health: starting` with `executable file not found`.
- Prevention rule: validate tool availability inside observability images before adding health checks; use startup ordering for minimal images and verify readiness from the host with the documented HTTP endpoint.

## 2026-07-26 — OTLP HTTP endpoint and runtime dependency alignment

- Failure mode: pointing Spring Boot's HTTP OTLP exporter at Tempo's gRPC port, then hitting an Okio binary mismatch between the exporter and another resolved runtime version.
- Detection signal: `Connection reset` after startup and `NoSuchMethodError: okio.Okio.socket(java.net.Socket)` in the API logs.
- Prevention rule: match the exporter protocol to the receiver (`4318/v1/traces` for OTLP/HTTP), inspect the runtime dependency tree, and pin the compatible `okio-jvm` version when the managed graph leaves the exporter vulnerable to linkage conflicts.
