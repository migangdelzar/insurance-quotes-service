package com.clara.insurancequotes.shared.error;

import com.clara.libs.i18n.MessageResolver;
import com.clara.libs.i18n.SupportedLocale;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final MessageResolver messages;

    public GlobalExceptionHandler(MessageResolver messages) {
        this.messages = messages;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException exception, HttpServletRequest request) {
        var locale = SupportedLocale.fromHeader(request.getHeader("Accept-Language"));
        var message = messages.resolve("error." + exception.code(), locale);
        var error = ApiError.of(exception.status().value(), exception.code(), message);
        return ResponseEntity.status(exception.status()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        var fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldValidationError(error.getField(), error.getDefaultMessage()))
                .toList();
        var locale = SupportedLocale.fromHeader(request.getHeader("Accept-Language"));
        var message = messages.resolve("error.VALIDATION_FAILED", locale);
        return ResponseEntity.badRequest().body(ApiError.validation(fieldErrors, message));
    }

    @ExceptionHandler(InvalidApiVersionException.class)
    public ResponseEntity<ApiError> handleInvalidApiVersion(
            InvalidApiVersionException exception, HttpServletRequest request) {
        var locale = SupportedLocale.fromHeader(request.getHeader("Accept-Language"));
        var message = messages.resolve("error.API_VERSION_INVALID", locale);
        return ResponseEntity.badRequest().body(ApiError.of(400, "API_VERSION_INVALID", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception", exception);
        var locale = SupportedLocale.fromHeader(request.getHeader("Accept-Language"));
        var message = messages.resolve("error.INTERNAL_ERROR", locale);
        var error = ApiError.of(500, "INTERNAL_ERROR", message);
        return ResponseEntity.internalServerError().body(error);
    }
}
