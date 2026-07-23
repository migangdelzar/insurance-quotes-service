package com.clara.insurancequotes.quote.api;

import com.clara.insurancequotes.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class IncompleteQuoteException extends ApiException {

    public IncompleteQuoteException(UUID quoteId, String missing) {
        super(
                "QUOTE_INCOMPLETE",
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Quote %s cannot be submitted: %s".formatted(quoteId, missing));
    }
}
