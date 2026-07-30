package com.clara.insurancequotes.quote.api.usecase;

import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import java.util.UUID;

/** Verifies that an owner's quote may be submitted. */
public interface EnsureQuoteSubmittableUseCase {

    QuoteDetails ensureSubmittable(UUID id, UUID ownerId);
}
