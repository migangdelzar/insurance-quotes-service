package com.clara.insurancequotes.quote.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvalidStateTransitionExceptionTest {

    @Test
    void message_describesQuoteStateAndAttemptedOperation() {
        var quoteId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        var exception = new InvalidStateTransitionException(quoteId, "SUBMITTED", "COVERAGE_UPDATE");

        assertThat(exception.getMessage())
                .isEqualTo(
                        "Quote 00000000-0000-0000-0000-000000000001 in status SUBMITTED does not allow COVERAGE_UPDATE");
    }
}
