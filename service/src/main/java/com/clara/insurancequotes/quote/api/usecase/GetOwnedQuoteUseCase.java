package com.clara.insurancequotes.quote.api.usecase;

import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import java.util.UUID;

/** Retrieves a quote only when it belongs to the supplied owner. */
public interface GetOwnedQuoteUseCase {

    QuoteDetails getOwnedQuote(UUID id, UUID ownerId);
}
