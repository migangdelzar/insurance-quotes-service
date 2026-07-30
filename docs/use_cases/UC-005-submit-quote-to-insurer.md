# Use Case: Submit Quote to Insurer

## Overview

**Use Case ID:** UC-005
**Use Case Name:** Submit Quote to Insurer
**Primary Actor:** Registered User
**Secondary Actor:** Insurer
**Goal:** A signed-in user submits their priced, complete quote to the insurer for underwriting and learns whether it was accepted.
**Status:** Implemented

## Preconditions

- A quote exists with a coverage tier selected and a current premium calculated (see UC-004).
- The user is signed in (UC-001).

## Main Success Scenario

1. The user asks to submit the quote.
2. The system verifies the quote is complete and in a submittable state.
3. The system sends the quote to the insurer for underwriting.
4. The insurer accepts the quote.
5. The system marks the quote as submitted and records that the submission happened.
6. The system confirms the successful submission to the user.

## Alternative Flows

### A1: Quote Already Submitted (Idempotent Resubmission)

**Trigger:** the quote is already in the Submitted status (step 2)
**Flow:**

1. The system does not contact the insurer again and returns the quote's current state as-is.
2. Use case ends successfully.

### A2: Quote Incomplete or Not Submittable

**Trigger:** the quote has no coverage tier selected, or its status does not allow submission (e.g. Expired) (step 2)
**Flow:**

1. The system rejects the submission, explaining why the quote cannot be submitted yet.
2. Use case ends; the user must complete the quote (UC-004) or, if expired, start a new one (UC-003).

### A3: Insurer Unavailable, Rejects, or Times Out

**Trigger:** the insurer does not respond successfully, rejects the request, or the call times out (step 3/4)
**Flow:**

1. The system marks the quote as submission-failed, keeping it eligible for another attempt.
2. The system reports the failure to the user with guidance that they may retry.
3. Use case ends; the user may retry from step 1.

## Postconditions

### Success Postconditions

- The quote's status is Submitted.
- A submission record is durably retained so downstream processing can rely on it having happened exactly once.
- The user sees a successful submission outcome.

### Failure Postconditions

- The quote's status is Submission-Failed (A3) and remains eligible for retry, or the quote is left unchanged when the request was rejected outright (A2).

## Business Rules

### BR-016: Coverage Must Be Selected Before Submission

A quote cannot be submitted until a coverage tier has been selected.

### BR-017: Submittable States

A quote can only be submitted while it is in the Draft or Submission-Failed status; an Expired quote cannot be submitted.

### BR-018: Resubmission Is Idempotent

Submitting a quote that is already Submitted does not contact the insurer again and simply returns the current, unchanged quote.

### BR-019: Failed Submissions Remain Retryable

If the insurer call fails, rejects, or times out, the quote moves to Submission-Failed rather than being lost, and the user can attempt submission again.

### BR-024: Submission Is Owner-Scoped, Admin Included

Only the quote's owner can edit or submit it. Administrators may inspect another user's quote through read-only oversight, but cannot submit or mutate that quote on the user's behalf.
