# Use Case: Select Coverage and Calculate Premium

## Overview

**Use Case ID:** UC-004
**Use Case Name:** Select Coverage and Calculate Premium
**Primary Actor:** Registered User
**Goal:** A signed-in user with a draft quote selects a coverage tier, answers any required health questions, and receives an up-to-date, system-calculated monthly premium.
**Status:** Implemented

## Preconditions

- A draft quote exists for the user, in the Draft or Submission-Failed status (see UC-003).
- The user is signed in (see UC-001).

## Main Success Scenario

1. The user selects a coverage tier (Basic, Standard, or Premium).
2. If the user is over 65, the user may also answer health questions (preexisting conditions and which ones, prescription medication use, tobacco use) and indicate whether spouse coverage is needed.
3. The system calculates the monthly premium for the current selections and updates the quote.
4. The system displays the calculated premium to the user.
5. Whenever the user changes the coverage tier, health answers, or spouse coverage choice, the system recalculates the premium (steps 3–4 repeat) so the displayed premium always reflects the current selections.
6. The user reviews the final summary, ready to submit (see UC-005).

## Alternative Flows

### A1: Health Data Submitted for a Non-Senior Applicant

**Trigger:** health questions or spouse coverage data are submitted for an applicant aged 65 or younger (step 2/3)
**Flow:**

1. The system rejects the coverage update, explaining that health data is only accepted for applicants over 65.
2. Use case continues at step 1 with the previous coverage selection unchanged.

### A2: Coverage Update Attempted on a Non-Editable Quote

**Trigger:** the quote is no longer in a state that allows coverage changes, e.g. it has already been submitted or has expired (step 1/3)
**Flow:**

1. The system rejects the change, reporting that the quote's current status does not allow it.
2. Use case ends; the user must start a new quote (UC-003) if they want different coverage.

## Postconditions

### Success Postconditions

- The quote has a selected coverage tier, any applicable health answers, and a current system-calculated monthly premium.
- The quote is ready to be submitted (UC-005).

### Failure Postconditions

- The quote's coverage is left at its last valid value (A1, A2).

## Business Rules

### BR-011: Health Data Restricted to Seniors

Health questions, prescription medication, tobacco use, and spouse coverage data are only accepted when the applicant's age is over 65; supplying any of this data for an applicant 65 or younger is rejected.

### BR-012: Coverage Tier Base Premium

Each coverage tier has a fixed base monthly premium: Basic $50, Standard $100, Premium $200.

### BR-013: Premium Rating Factors

The monthly premium is the coverage tier's base premium multiplied by all applicable rating factors: 1.5 for an applicant over 65, 1.3 if any preexisting condition is reported, 1.2 if the applicant uses tobacco, and 1.4 if spouse coverage is requested. A factor that does not apply contributes a multiplier of 1 (e.g. a Standard quote for a 70-year-old with a preexisting condition, tobacco use, and spouse coverage prices at $100 × 1.5 × 1.3 × 1.2 × 1.4 = $327.60).

### BR-014: Premium Is Always Server-Calculated

The displayed monthly premium always reflects a calculation performed by the system, rounded to 2 decimal places; it is never computed or guessed by the user's device.

### BR-015: Coverage Editable Only While Draft or Failed

Coverage, health answers, and spouse coverage can only be changed while the quote is in the Draft or Submission-Failed status.
