package com.clara.insurancequotes.quote.api.usecase;

import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import java.util.UUID;

/** Records a failed insurer submission for an owner's quote. */
public interface MarkQuoteSubmissionFailedUseCase {

    QuoteDetails markSubmissionFailed(UUID id, UUID ownerId);
}
