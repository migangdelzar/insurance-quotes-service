package com.clara.insurancequotes.submission.application.exception;

public abstract class SubmissionException extends RuntimeException {

    protected SubmissionException(String message) {
        super(message);
    }
}
