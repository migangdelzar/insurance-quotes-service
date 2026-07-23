package com.clara.insurancequotes.quote.adapter.in.web.advice;

import com.clara.insurancequotes.quote.application.exception.QuoteApplicationException;
import com.clara.insurancequotes.quote.application.exception.QuoteNotFoundException;
import com.clara.insurancequotes.quote.domain.exception.HealthDataNotAllowedException;
import com.clara.insurancequotes.quote.domain.exception.IncompleteQuoteException;
import com.clara.insurancequotes.quote.domain.exception.InvalidStateTransitionException;
import com.clara.insurancequotes.quote.domain.exception.QuoteException;
import com.clara.insurancequotes.shared.error.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class QuoteExceptionHandler {

    @ExceptionHandler({QuoteException.class, QuoteApplicationException.class})
    public ResponseEntity<ApiError> handle(RuntimeException exception) {
        var mapped = map(exception);
        return ResponseEntity.status(mapped.status())
                .body(ApiError.of(mapped.status(), mapped.code(), exception.getMessage()));
    }

    private static MappedError map(RuntimeException exception) {
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
        return new MappedError(500, "INTERNAL_ERROR");
    }

    private record MappedError(int status, String code) {}
}
