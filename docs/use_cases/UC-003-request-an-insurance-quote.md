# Use Case: Request an Insurance Quote

## Overview

**Use Case ID:** UC-003
**Use Case Name:** Request an Insurance Quote
**Primary Actor:** Registered User
**Goal:** A signed-in user enters their personal details and coverage preferences and receives an up-to-date, system-calculated monthly premium for a draft quote.
**Status:** Implemented

## Preconditions

- The user is signed in (see UC-001).

## Main Success Scenario

1. The user enters their personal details: name, email, age, and ZIP code.
2. The system validates the details and creates a new draft quote.
3. The user selects a coverage tier (Basic, Standard, or Premium).
4. If the user is over 65, the user may also answer health questions (preexisting conditions and which ones, prescription medication use, tobacco use) and indicate whether spouse coverage is needed.
5. The system calculates the monthly premium for the current selections and updates the draft quote.
6. The system displays the calculated premium to the user.
7. Whenever the user changes the coverage tier, health answers, or spouse coverage choice, the system recalculates the premium (steps 5–6 repeat) so the displayed premium always reflects the current selections.
8. The user reviews the final summary, ready to submit (see UC-004).

## Alternative Flows

### A1: Personal Details Are Invalid

**Trigger:** required personal details are missing or malformed (step 2)
**Flow:**

1. The system rejects the request and reports which fields are invalid.
2. Use case continues at step 1.

### A2: Health Data Submitted for a Non-Senior Applicant

**Trigger:** health questions or spouse coverage data are submitted for an applicant aged 65 or younger (step 4/5)
**Flow:**

1. The system rejects the coverage update, explaining that health data is only accepted for applicants over 65.
2. Use case continues at step 3 with the previous coverage selection unchanged.

### A3: Personal Details Are Edited After a Draft Exists

**Trigger:** the user changes personal details (step 1) after a draft quote already exists
**Flow:**

1. The system starts a new draft quote with the updated personal details, distinct from the earlier draft.
2. Use case continues at step 3.
3. The abandoned earlier draft remains subject to automatic expiration (see UC-006).

### A4: Coverage Update Attempted on a Non-Editable Quote

**Trigger:** the quote is no longer in a state that allows coverage changes, e.g. it has already been submitted or has expired (step 3/5)
**Flow:**

1. The system rejects the change, reporting that the quote's current status does not allow it.
2. Use case ends; the user must start a new quote (A3) if they want different coverage.

## Postconditions

### Success Postconditions

- A draft quote exists with the user's personal details, selected coverage, any applicable health answers, and a current system-calculated monthly premium.
- The draft quote is owned by the requesting user and is not visible to other regular users.
- The quote is ready to be submitted (UC-004).

### Failure Postconditions

- No draft quote is created (A1), or the existing draft quote's coverage is left at its last valid value (A2, A4).

## Business Rules

### BR-009: Applicant Age Range

The applicant's age must be between 18 and 120 inclusive.

### BR-010: ZIP Code Format

The ZIP code must be a 5-digit value.

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

### BR-023: Quotes Are Owned by Their Creator

Every quote is associated with the authenticated user who created it. Regular users can read and modify only their own quotes; ownership is derived from the authenticated session and never accepted from the request body.
