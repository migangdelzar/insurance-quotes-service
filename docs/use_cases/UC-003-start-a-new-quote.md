# Use Case: Start a New Quote

## Overview

**Use Case ID:** UC-003
**Use Case Name:** Start a New Quote
**Primary Actor:** Registered User
**Goal:** A signed-in user submits their personal details to create a new draft quote, ready for coverage selection.
**Status:** Implemented

## Preconditions

- The user is signed in (see UC-001).

## Main Success Scenario

1. The user enters their personal details: name, email, age, and ZIP code.
2. The system validates the details.
3. The system creates a new draft quote owned by the user.
4. The system confirms the draft quote was created and is ready for coverage selection (see UC-004).

## Alternative Flows

### A1: Personal Details Are Invalid

**Trigger:** required personal details are missing or malformed (step 2)
**Flow:**

1. The system rejects the request and reports which fields are invalid.
2. Use case continues at step 1.

### A2: Personal Details Are Edited After a Draft Exists

**Trigger:** the user changes personal details (step 1) after a draft quote already exists
**Flow:**

1. The system starts a new draft quote with the updated personal details, distinct from the earlier draft.
2. Use case continues at step 3.
3. The abandoned earlier draft remains subject to automatic expiration (see UC-007).

## Postconditions

### Success Postconditions

- A draft quote exists with the user's personal details and no coverage selected yet.
- The draft quote is owned by the requesting user and is not visible to other regular users.
- The quote is ready for coverage selection (UC-004).

### Failure Postconditions

- No draft quote is created (A1).

## Business Rules

### BR-009: Applicant Age Range

The applicant's age must be between 18 and 120 inclusive.

### BR-010: ZIP Code Format

The ZIP code must be a 5-digit value.

### BR-023: Quotes Are Owned by Their Creator

Every quote is associated with the authenticated user who created it. Regular users can read and modify only their own quotes; ownership is derived from the authenticated session and never accepted from the request body.
