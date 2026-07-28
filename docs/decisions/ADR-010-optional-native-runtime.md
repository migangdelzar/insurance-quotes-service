# ADR-010: Native image as an optional runtime profile

## Status

Accepted

## Date

2026-07-28

## Decision

Keep the Java 17 JVM image and Compose path as the default reviewer and deployment workflow. Provide a separate Spring Boot native profile and Compose file for explicit startup, memory, and image-size comparison.

Java 25 is build-only tooling for the Spring Boot 4/Paketo native builder; the application runtime contract remains Java 17.

## Context and decision drivers

Native compilation may reduce startup time and memory, but it is slower, resource-sensitive, and more environment-dependent than the JVM build. The challenge must remain easy to run on a standard Java 17 environment.

## Considered alternatives

- **Native by default:** rejected because multi-minute, memory-intensive builds would make the critical development path fragile.
- **JVM only:** rejected because the project benefits from a reproducible optional comparison path for serverless/container evaluation.
- **Java 25 runtime:** rejected because Java 17 is the required application runtime floor.

## Implementation evidence

- \`service/pom.xml\` native profile
- \`deployment/compose/docker-compose.native.yml\`
- \`scripts/compare-runtimes.sh\`
- \`.github/workflows/native-comparison.yml\`
- \`README.md\` JVM/native comparison section

## Consequences

### Positive

- Java 17 remains the predictable local and CI runtime.
- Native behavior can be measured without changing the default deployment.
- The comparison script makes startup, RSS, elapsed time, and image size explicit rather than speculative.

### Negative and operational

- Native compilation needs more memory and a compatible cross-compilation builder.
- Native reachability/configuration must be maintained as dependencies change.
- Native and JVM images need separate smoke verification.

## Related decisions

- [ADR-001: Module boundaries](ADR-001-spring-modulith-package-boundaries.md)
- [ADR-009: Observability](ADR-009-observability-stack.md)
