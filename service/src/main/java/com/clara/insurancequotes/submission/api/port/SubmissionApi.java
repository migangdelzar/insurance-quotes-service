package com.clara.insurancequotes.submission.api.port;

import com.clara.insurancequotes.quote.api.model.QuoteView;
import java.util.UUID;

public interface SubmissionApi {

    QuoteView submit(UUID quoteId);
}
