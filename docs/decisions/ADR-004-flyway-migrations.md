# ADR-004: Flyway over Liquibase
Date: 2026-07-22 · Status: Accepted

## Context
Schema evolution must be explicit, reviewable, reproducible in containers, and compatible with the optional native image.

## Decision
Use ordered Flyway SQL migrations for the PostgreSQL schema.

## Consequences
SQL remains close to the deployed database and is easy to inspect in review. Liquibase was rejected because the selected approach has a smaller migration surface and a simpler native-image story for this service.
