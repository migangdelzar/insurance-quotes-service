package com.clara.insurancequotes.quote.api.usecase;

import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.result.QuoteView;
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
