# Use Case: Authenticate

## Overview

**Use Case ID:** UC-001
**Use Case Name:** Authenticate
**Primary Actor:** Visitor / Registered User
**Goal:** A visitor establishes a signed-in session (or renews/ends one) so they can access their quotes.
**Status:** Implemented

## Preconditions

- The visitor holds an account (username and password) provisioned in the system.
- For passkey sign-in, a passkey has already been registered for the account (see UC-002).

## Main Success Scenario

1. The visitor enters their username and password.
2. The system verifies the credentials.
3. The system signs the visitor in and issues an access session and a renewable session token.
4. The system grants access to the visitor's quotes.

## Alternative Flows

### A1: Passkey Is Registered — Multi-Factor Step Required

**Trigger:** the account has a registered passkey (step 3)
**Flow:**

1. Instead of signing the user in directly, the system issues a short-lived, single-purpose confirmation token and asks for a second factor.
2. The user completes a passkey confirmation (biometric/security key) using that token.
3. The system verifies the passkey confirmation is tied to the pending token and to the correct account.
4. Use case continues at step 3 (session issued).

### A2: Invalid Credentials

**Trigger:** the username is unknown or the password does not match (step 2)
**Flow:**

1. The system rejects the attempt with an invalid-credentials error.
2. Use case ends.

### A3: Passwordless Sign-In

**Trigger:** the visitor chooses to sign in with a passkey instead of a password
**Flow:**

1. The visitor optionally identifies their account and starts a passkey confirmation.
2. If the account has no registered passkey, the system rejects the attempt and directs the visitor to sign in with a password first.
3. Otherwise the visitor completes the passkey confirmation.
4. The system verifies the confirmation and identifies the account.
5. Use case continues at step 3 (session issued).

### A4: Renew an Expiring Session

**Trigger:** the user's access session is nearing expiry and a valid renewable session token is available
**Flow:**

1. The system exchanges the renewable session token for a new access session and a new renewable session token.
2. The previous renewable session token is invalidated.
3. Use case ends (session renewed).

### A5: Reused or Expired Renewable Session Token

**Trigger:** the renewable session token presented in A4 was already used once before, or has expired (step 1 of A4)
**Flow:**

1. The system rejects the renewal.
2. If the token had already been used once before (replay), the system also invalidates every other renewable token issued in the same session lineage, forcing full re-authentication.
3. Use case ends; the user must sign in again (main flow).

### A6: Sign Out

**Trigger:** the user chooses to end their session
**Flow:**

1. The system invalidates the presented renewable session token.
2. Use case ends.

## Postconditions

### Success Postconditions

- The visitor holds a time-limited access session and a renewable session token scoped to their account.
- The system can identify the account on every subsequent request until the access session expires.

### Failure Postconditions

- No session is issued.
- The account's existing sessions are unaffected, except in A5 where the entire session lineage is invalidated.

## Business Rules

### BR-001: Valid Credentials Required

A username and password must both be supplied and match a provisioned account; otherwise the attempt is rejected.

### BR-002: Passkey Presence Forces a Second Factor

If the account has at least one registered passkey, a successful password check alone does not grant a session — a passkey confirmation is also required.

### BR-003: Confirmation Token Is Single-Purpose and Short-Lived

The token issued to bridge password verification and passkey confirmation is valid only for that purpose and expires approximately 5 minutes after issuance.

### BR-004: Access Session Lifetime

An access session is valid for approximately 30 minutes from issuance.

### BR-005: Renewable Session Rotation and Reuse Detection

Each renewal issues a new renewable session token and invalidates the one just used; presenting an already-used renewable token again invalidates the entire lineage of tokens descended from the same original sign-in.

### BR-006: Passwordless Sign-In Requires a Registered Passkey

Attempting passwordless sign-in for an account with no registered passkey is rejected with guidance to sign in with a password first.
