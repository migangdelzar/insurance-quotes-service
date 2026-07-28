package com.clara.insurancequotes.quote.api.usecase;

import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.api.result.QuotePageView;
import com.clara.insurancequotes.quote.api.result.QuoteView;
import java.util.UUID;

public interface QuoteApi {

    QuoteView create(CreateQuoteCommand command);

    QuoteView updateCoverage(UUID id, UpdateCoverageCommand command);

    QuoteView getQuote(UUID id);

    QuotePageView listQuotes(QuoteQuery query);

    QuoteView ensureSubmittable(UUID id);

    QuoteView markSubmitted(UUID id);

    QuoteView markSubmissionFailed(UUID id);
}
