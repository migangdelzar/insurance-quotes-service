package com.clara.insurancequotes.auth.adapter.in.web.advice;

import com.clara.insurancequotes.auth.api.exception.AuthException;
import com.clara.insurancequotes.auth.api.exception.InvalidCredentialsException;
import com.clara.insurancequotes.auth.api.exception.InvalidPasskeyException;
import com.clara.insurancequotes.shared.error.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiError> handle(AuthException exception) {
        if (exception instanceof InvalidCredentialsException) {
            return ResponseEntity.status(401)
                    .body(ApiError.of(401, "AUTH_INVALID_CREDENTIALS", exception.getMessage()));
        }
        if (exception instanceof InvalidPasskeyException) {
            return ResponseEntity.status(401).body(ApiError.of(401, "AUTH_INVALID_PASSKEY", exception.getMessage()));
        }
        return ResponseEntity.internalServerError().body(ApiError.of(500, "INTERNAL_ERROR", exception.getMessage()));
    }
}
