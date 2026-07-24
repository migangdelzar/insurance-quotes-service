# ADR-008: Metrics and structured logs as the first observability tier
Date: 2026-07-22 · Status: Accepted

## Context
The service needs actionable operational signals for correlation, insurer latency, submissions, and HTTP failures without introducing a large platform dependency.

## Decision
Provide correlation IDs, JSON logs, Micrometer business metrics, Prometheus export, and a provisioned Grafana dashboard.

## Consequences
The stack is useful for the challenge’s single-service runtime and leaves room for tracing later. Zipkin, Loki, and an OpenTelemetry collector were rejected as first-cut infrastructure that would add operational indirection before multiple services justify it.
