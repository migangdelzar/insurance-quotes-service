package com.clara.insurancequotes.error;

import com.clara.insurancequotes.auth.application.exception.AuthException;
import com.clara.insurancequotes.auth.application.exception.InvalidCredentialsException;
import com.clara.insurancequotes.quote.application.exception.QuoteApplicationException;
import com.clara.insurancequotes.quote.application.exception.QuoteNotFoundException;
import com.clara.insurancequotes.quote.domain.exception.HealthDataNotAllowedException;
import com.clara.insurancequotes.quote.domain.exception.IncompleteQuoteException;
import com.clara.insurancequotes.quote.domain.exception.InvalidStateTransitionException;
import com.clara.insurancequotes.quote.domain.exception.QuoteException;
import com.clara.insurancequotes.submission.application.exception.InsurerUnavailableException;
import com.clara.insurancequotes.submission.application.exception.SubmissionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException exception) {
        var error = ApiError.of(exception.status().value(), exception.code(), exception.getMessage());
        return ResponseEntity.status(exception.status()).body(error);
    }

    @ExceptionHandler({
        QuoteException.class,
        QuoteApplicationException.class,
        AuthException.class,
        SubmissionException.class
    })
    public ResponseEntity<ApiError> handleModuleException(RuntimeException exception) {
        var mapped = mapModuleException(exception);
        return ResponseEntity.status(mapped.status())
                .body(ApiError.of(mapped.status(), mapped.code(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        var fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldValidationError(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(ApiError.validation(fieldErrors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        var error = ApiError.of(500, "INTERNAL_ERROR", "An unexpected error occurred");
        return ResponseEntity.internalServerError().body(error);
    }

    private static MappedError mapModuleException(RuntimeException exception) {
        if (exception instanceof QuoteNotFoundException) {
            return new MappedError(404, "QUOTE_NOT_FOUND");
        }
        if (exception instanceof InvalidStateTransitionException) {
            return new MappedError(409, "QUOTE_INVALID_STATE_TRANSITION");
        }
        if (exception instanceof HealthDataNotAllowedException) {
            return new MappedError(422, "QUOTE_HEALTH_DATA_NOT_ALLOWED");
        }
        if (exception instanceof IncompleteQuoteException) {
            return new MappedError(422, "QUOTE_INCOMPLETE");
        }
        if (exception instanceof InvalidCredentialsException) {
            return new MappedError(401, "AUTH_INVALID_CREDENTIALS");
        }
        if (exception instanceof InsurerUnavailableException) {
            return new MappedError(502, "INSURER_UNAVAILABLE");
        }
        return new MappedError(500, "INTERNAL_ERROR");
    }

    private record MappedError(int status, String code) {}
}
