package com.clara.insurancequotes.submission.api.usecase;

import com.clara.insurancequotes.quote.api.result.QuoteView;
import java.util.UUID;

public interface SubmissionApi {

    QuoteView submit(UUID quoteId, UUID ownerId);
}
