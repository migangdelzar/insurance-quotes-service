package com.clara.insurancequotes.quote.api.exception;

import java.util.UUID;

/** Expected public failure when a quote is not visible to the caller. */
public class QuoteNotFoundException extends RuntimeException {

    public QuoteNotFoundException(UUID quoteId) {
        super("Quote %s not found".formatted(quoteId));
    }
}
