# ADR-002: Outbox event publication to Kafka
Date: 2026-07-22 · Status: Accepted

## Context
Successful quote submission must publish an event without losing it when Kafka is temporarily unavailable.

## Decision
Use Spring Modulith’s event registry and `@Externalized` publication so the event record is written in the same transaction and delivered to Kafka afterward.

## Consequences
The database is the durable handoff and Kafka remains the external transport. Spring Cloud Stream was rejected because one producer-only flow does not justify a binder; a plain `KafkaTemplate` was rejected because it could lose events between database commit and broker publication.
