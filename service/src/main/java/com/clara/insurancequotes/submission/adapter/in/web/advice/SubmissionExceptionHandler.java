package com.clara.insurancequotes.submission.adapter.in.web.advice;

import com.clara.insurancequotes.shared.error.ApiError;
import com.clara.insurancequotes.submission.api.exception.InsurerUnavailableException;
import com.clara.insurancequotes.submission.api.exception.SubmissionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class SubmissionExceptionHandler {

    @ExceptionHandler(SubmissionException.class)
    public ResponseEntity<ApiError> handle(SubmissionException exception) {
        if (exception instanceof InsurerUnavailableException) {
            return ResponseEntity.status(502).body(ApiError.of(502, "INSURER_UNAVAILABLE", exception.getMessage()));
        }
        log.error("Unexpected submission failure", exception);
        return ResponseEntity.internalServerError()
                .body(ApiError.of(
                        500, "INTERNAL_ERROR", "An unexpected error occurred while processing the submission."));
    }
}
