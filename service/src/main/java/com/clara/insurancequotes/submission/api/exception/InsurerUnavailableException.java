package com.clara.insurancequotes.submission.api.exception;

public class InsurerUnavailableException extends SubmissionException {

    public InsurerUnavailableException(String detail) {
        super("Insurer submission failed: %s. The quote can be resubmitted.".formatted(detail));
    }
}
