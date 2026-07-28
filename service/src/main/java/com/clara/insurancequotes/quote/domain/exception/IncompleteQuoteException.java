package com.clara.insurancequotes.quote.domain.exception;

import java.util.UUID;

public class IncompleteQuoteException extends QuoteException {

    public IncompleteQuoteException(UUID quoteId, String missing) {
        super("Quote %s cannot be submitted: %s".formatted(quoteId, missing));
    }
}
