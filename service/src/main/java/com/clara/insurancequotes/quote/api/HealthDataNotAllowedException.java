package com.clara.insurancequotes.quote.api;

import com.clara.insurancequotes.error.ApiException;
import org.springframework.http.HttpStatus;

public class HealthDataNotAllowedException extends ApiException {

    public HealthDataNotAllowedException(int age) {
        super(
                "QUOTE_HEALTH_DATA_NOT_ALLOWED",
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Health data is only accepted when age > 65 (age was %d)".formatted(age));
    }
}
