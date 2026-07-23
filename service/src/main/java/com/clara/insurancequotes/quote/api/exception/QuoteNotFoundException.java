package com.clara.insurancequotes.quote.api.exception;

import java.util.UUID;

public class QuoteNotFoundException extends QuoteApplicationException {

    public QuoteNotFoundException(UUID quoteId) {
        super("Quote %s not found".formatted(quoteId));
    }
}
