package com.clara.insurancequotes.quote.api;

import com.clara.insurancequotes.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class QuoteNotFoundException extends ApiException {

    public QuoteNotFoundException(UUID quoteId) {
        super("QUOTE_NOT_FOUND", HttpStatus.NOT_FOUND, "Quote %s not found".formatted(quoteId));
    }
}
