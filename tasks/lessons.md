# Lessons

## 2026-07-23 — package responsibility correction

- Failure mode: treating every exception as an API exception made the package structure imply that application and domain failures belonged to the transport layer.
- Detection signal: `QuoteNotFoundException` represented a use-case lookup failure, while `ApiException` represented HTTP concerns.
- Prevention rule: classify exceptions by responsibility first; keep domain/application exceptions framework-neutral and translate them only at inbound adapter boundaries.

## 2026-07-23 — framework-native API versioning

- Failure mode: adding `/api/v1` directly to controller paths before checking whether the selected Spring Framework version supports declarative API version conditions.
- Detection signal: Spring Framework 6.2.8's `@RequestMapping` has no `version` attribute; native API versioning requires Spring Framework 7 and Spring Boot 4.
- Prevention rule: verify framework feature availability against the resolved dependency versions before implementing an adapter-level convention; use Spring's `WebMvcConfigurer.configureApiVersioning` and version-aware mappings when the native feature is a requirement.
