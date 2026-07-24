# ADR-009: Native image as an optional runtime profile
Date: 2026-07-22 · Status: Accepted

## Context
Native compilation can reduce startup time and memory, but it is slower and more environment-sensitive than the JVM build.

## Decision
Keep the JVM image and Compose path as the default reviewer workflow, while providing a separate Spring Boot native profile and Compose file.

## Consequences
Reviewers get a predictable path and can compare native behavior and footprint explicitly. Native-by-default was rejected because multi-minute platform builds would put the critical development path behind an unnecessary toolchain cost.
