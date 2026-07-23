package com.clara.insurancequotes.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        List<FieldValidationError> fieldErrors,
        String traceId) {

    public record FieldValidationError(String field, String message) {}

    public static ApiError of(int status, String code, String message) {
        return new ApiError(Instant.now(), status, code, message, null, currentTraceId());
    }

    public static ApiError validation(List<FieldValidationError> fieldErrors) {
        return new ApiError(
                Instant.now(), 400, "VALIDATION_FAILED", "Request validation failed", fieldErrors, currentTraceId());
    }

    private static String currentTraceId() {
        return org.slf4j.MDC.get("correlationId");
    }
}
