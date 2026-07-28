package com.clara.insurancequotes.submission.api.exception;

public abstract class SubmissionException extends RuntimeException {

    protected SubmissionException(String message) {
        super(message);
    }
}
