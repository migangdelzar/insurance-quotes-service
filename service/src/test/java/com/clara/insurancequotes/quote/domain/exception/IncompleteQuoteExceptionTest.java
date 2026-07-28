package com.clara.insurancequotes.quote.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class IncompleteQuoteExceptionTest {

    @Test
    void message_identifiesMissingQuoteData() {
        var quoteId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        var exception = new IncompleteQuoteException(quoteId, "coverage type not selected");

        assertThat(exception.getMessage())
                .isEqualTo(
                        "Quote 00000000-0000-0000-0000-000000000001 cannot be submitted: coverage type not selected");
    }
}
