package com.clara.insurancequotes.quote.application.exception;

import java.util.UUID;

public class QuoteNotFoundException extends QuoteApplicationException {

    public QuoteNotFoundException(UUID quoteId) {
        super("Quote %s not found".formatted(quoteId));
    }
}
