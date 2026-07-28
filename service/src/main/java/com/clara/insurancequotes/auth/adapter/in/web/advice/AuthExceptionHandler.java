package com.clara.insurancequotes.auth.adapter.in.web.advice;

import com.clara.insurancequotes.auth.api.exception.AuthException;
import com.clara.insurancequotes.auth.api.exception.InvalidCredentialsException;
import com.clara.insurancequotes.auth.api.exception.InvalidPasskeyException;
import com.clara.insurancequotes.auth.api.exception.PasskeyNotRegisteredException;
import com.clara.insurancequotes.shared.error.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class AuthExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiError> handle(AuthException exception) {
        if (exception instanceof InvalidCredentialsException) {
            return ResponseEntity.status(401)
                    .body(ApiError.of(401, "AUTH_INVALID_CREDENTIALS", exception.getMessage()));
        }
        if (exception instanceof InvalidPasskeyException) {
            log.warn("Passkey authentication failed", exception);
            return ResponseEntity.status(401).body(ApiError.of(401, "AUTH_INVALID_PASSKEY", exception.getMessage()));
        }
        if (exception instanceof PasskeyNotRegisteredException) {
            return ResponseEntity.status(409)
                    .body(ApiError.of(409, "AUTH_PASSKEY_NOT_REGISTERED", exception.getMessage()));
        }
        return ResponseEntity.internalServerError().body(ApiError.of(500, "INTERNAL_ERROR", exception.getMessage()));
    }
}
