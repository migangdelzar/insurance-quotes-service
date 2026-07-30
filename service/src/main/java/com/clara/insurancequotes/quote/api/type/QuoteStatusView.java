package com.clara.insurancequotes.quote.api.type;

/** Stable public representation of a quote lifecycle status. */
public enum QuoteStatusView {
    DRAFT,
    SUBMITTED,
    SUBMISSION_FAILED,
    EXPIRED;

    public boolean alreadySubmitted() {
        return this == SUBMITTED;
    }
}
