package com.clara.insurancequotes.quote.api.usecase;

import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import java.util.UUID;

/** Marks an owner's quote as submitted after insurer acceptance. */
public interface MarkQuoteSubmittedUseCase {

    QuoteDetails markSubmitted(UUID id, UUID ownerId);
}
