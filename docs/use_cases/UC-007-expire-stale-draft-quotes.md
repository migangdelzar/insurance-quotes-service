# Use Case: Expire Stale Draft Quotes

## Overview

**Use Case ID:** UC-007
**Use Case Name:** Expire Stale Draft Quotes
**Primary Actor:** Scheduler
**Goal:** The system automatically reclaims draft quotes that were abandoned for too long, so they stop cluttering active history and cannot be submitted later with stale pricing.
**Status:** Implemented

## Preconditions

- One or more draft quotes exist that have not been updated within the configured draft lifetime.

## Main Success Scenario

1. On its configured schedule, the system looks for draft quotes that have been inactive longer than the configured draft lifetime.
2. The system marks each such quote as expired.
3. The system records that each affected quote expired.

## Alternative Flows

### A1: No Stale Drafts Found

**Trigger:** no draft quote has exceeded the configured lifetime (step 1)
**Flow:**

1. The system takes no action.
2. Use case ends.

## Postconditions

### Success Postconditions

- Every draft quote inactive longer than the configured lifetime is now in the Expired status.
- Expired quotes can no longer be edited (UC-004) or submitted (UC-005).

### Failure Postconditions

- Not applicable; a run that finds nothing to expire is a normal successful outcome (A1).

## Business Rules

### BR-022: Draft Expiration Threshold

A draft quote is automatically expired once it has remained in the Draft status longer than the system's configured draft time-to-live.
