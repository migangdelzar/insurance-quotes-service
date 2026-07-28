package com.clara.insurancequotes.quote.domain.model;

public enum QuoteStatus {
    DRAFT(true, true, true, false),
    SUBMITTED(false, false, false, true),
    SUBMISSION_FAILED(false, true, false, false),
    EXPIRED(false, false, false, false);

    private final boolean allowsCoverageUpdate;
    private final boolean allowsSubmission;
    private final boolean allowsExpiration;
    private final boolean alreadySubmitted;

    QuoteStatus(
            boolean allowsCoverageUpdate,
            boolean allowsSubmission,
            boolean allowsExpiration,
            boolean alreadySubmitted) {
        this.allowsCoverageUpdate = allowsCoverageUpdate;
        this.allowsSubmission = allowsSubmission;
        this.allowsExpiration = allowsExpiration;
        this.alreadySubmitted = alreadySubmitted;
    }

    public boolean allowsCoverageUpdate() {
        return allowsCoverageUpdate;
    }

    public boolean allowsSubmission() {
        return allowsSubmission;
    }

    public boolean allowsExpiration() {
        return allowsExpiration;
    }

    public boolean alreadySubmitted() {
        return alreadySubmitted;
    }
}
