package com.clara.insurancequotes.quote.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HealthDataNotAllowedExceptionTest {

    @Test
    void message_explainsAgeRequirement() {
        var exception = new HealthDataNotAllowedException(34);

        assertThat(exception.getMessage()).isEqualTo("Health data is only accepted when age > 65 (age was 34)");
    }
}
