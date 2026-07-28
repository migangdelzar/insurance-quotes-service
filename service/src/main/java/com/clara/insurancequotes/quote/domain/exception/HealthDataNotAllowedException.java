package com.clara.insurancequotes.quote.domain.exception;

public class HealthDataNotAllowedException extends QuoteException {

    public HealthDataNotAllowedException(int age) {
        super("Health data is only accepted when age > 65 (age was %d)".formatted(age));
    }
}
