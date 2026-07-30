package com.clara.insurancequotes.submission.api.usecase;

import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import java.util.UUID;

public interface SubmissionApi {

    QuoteDetails submit(UUID quoteId, UUID ownerId);
}
