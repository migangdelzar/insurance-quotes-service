# Use Case: Review Quote History and Analytics

## Overview

**Use Case ID:** UC-005
**Use Case Name:** Review Quote History and Analytics
**Primary Actor:** Registered User / Administrator (read-only)
**Goal:** A signed-in user browses their past and in-progress quotes, or an administrator reviews quote activity across users, inspects details, and sees the applicable summary analytics.
**Status:** Implemented

## Preconditions

- The user is signed in (see UC-001).
- At least one quote has been created (UC-003) for the history and analytics to show data; an empty result is still a valid outcome.

## Main Success Scenario

1. The user opens their quote list.
2. The system returns a page of the user's quotes, most recent first.
3. The user narrows the list by searching, filtering by status or coverage tier, changing the sort order, or moving to another page.
4. The system returns the matching page of quotes accordingly.
5. The user selects a single quote to inspect.
6. The system displays that quote's full details, including its current status and premium.
7. The user views the analytics summary (totals by status, priced-quote count, total and average premium, submission rate, status/coverage distribution, and a recent trend).
8. The system returns the current analytics summary.

## Alternative Flows

### A1: Invalid Search, Filter, or Sort Parameters

**Trigger:** the requested filter, sort field, or paging parameters are not recognized (step 3/4)
**Flow:**

1. The system rejects the request, reporting that the query is invalid.
2. Use case continues at step 3 with the previous valid view retained.

### A2: Quote Not Found

**Trigger:** the selected quote does not exist (step 5/6)
**Flow:**

1. The system reports that the quote was not found.
2. Use case ends.

### A3: No Quotes Yet

**Trigger:** the user has not created any quotes (step 2)
**Flow:**

1. The system returns an empty list.
2. The user is invited to start a new quote (UC-003).
3. Use case ends.

## Postconditions

### Success Postconditions

- The user has seen the requested page of quotes, the detail of a selected quote, and/or the current analytics summary.
- No data is modified by this use case.

### Failure Postconditions

- No list, detail, or summary is returned (A1, A2); the user's quotes are unaffected either way.

## Business Rules

### BR-020: Listing Supports Search, Filter, Sort, and Pagination

The quote list can be searched, filtered by status and coverage tier, sorted by creation date, update date, name, premium, or status (ascending or descending), and paginated; requests using unsupported values are rejected rather than silently ignored.

### BR-021: Analytics Reflect All of the User's Quotes

The analytics summary (status/coverage distribution, premium totals and average, submission rate, and trend) is computed over the full set of the user's quotes, not just the currently displayed page.

### BR-025: Administrator Read-Only Oversight

Regular users receive list, detail, and analytics results only for quotes they created. An Administrator receives cross-user quote history and global analytics for oversight, but cannot create, edit, or submit another user's quote.
