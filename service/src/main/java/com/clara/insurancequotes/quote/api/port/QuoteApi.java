package com.clara.insurancequotes.quote.api.port;

import com.clara.insurancequotes.quote.api.model.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.model.QuoteView;
import com.clara.insurancequotes.quote.api.model.UpdateCoverageCommand;
import java.util.List;
import java.util.UUID;

public interface QuoteApi {

    QuoteView create(CreateQuoteCommand command);

    QuoteView updateCoverage(UUID id, UpdateCoverageCommand command);

    QuoteView getQuote(UUID id);

    List<QuoteView> listQuotes();

    QuoteView ensureSubmittable(UUID id);

    QuoteView markSubmitted(UUID id);

    QuoteView markSubmissionFailed(UUID id);
}
