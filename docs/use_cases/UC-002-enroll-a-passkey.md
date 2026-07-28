# Use Case: Enroll a Passkey

## Overview

**Use Case ID:** UC-002
**Use Case Name:** Enroll a Passkey
**Primary Actor:** Registered User
**Goal:** A signed-in user registers a passkey (biometric or security key) on their account so they can sign in faster and with a second factor.
**Status:** Implemented

## Preconditions

- The user is already signed in (see UC-001).

## Main Success Scenario

1. The user asks to add a passkey to their account.
2. The system starts a registration ceremony and returns the parameters the user's device needs to create a passkey.
3. The user's device creates a passkey and the user confirms it (biometric/security key prompt).
4. The system verifies the new passkey and attaches it to the user's account.
5. The system confirms the passkey is now available for future sign-ins.

## Alternative Flows

### A1: Ceremony Fails or Is Abandoned

**Trigger:** the device cannot complete the passkey creation, or verification fails (step 3 or 4)
**Flow:**

1. The system rejects the registration.
2. No passkey is attached to the account.
3. Use case ends.

## Postconditions

### Success Postconditions

- A new passkey credential is attached to the user's account.
- The account is now eligible for the multi-factor and passwordless sign-in flows described in UC-001.

### Failure Postconditions

- The account's set of registered passkeys is unchanged.

## Business Rules

### BR-007: Registration Requires an Existing Session

Only an already-authenticated user may start or complete passkey registration; it is not available to a visitor.

### BR-008: Each Passkey Credential Is Unique and User-Scoped

A registered passkey credential is uniquely identified and permanently associated with exactly one account.
