package com.clara.insurancequotes.quote.api.usecase;

import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.api.result.QuotePageView;
import com.clara.insurancequotes.quote.api.result.QuoteSummaryView;
import com.clara.insurancequotes.quote.api.result.QuoteView;
import java.util.UUID;

public interface QuoteApi {

    QuoteView create(CreateQuoteCommand command, UUID ownerId);

    QuoteView updateCoverage(UUID id, UpdateCoverageCommand command, UUID ownerId);

    QuoteView getQuote(UUID id, RequestingUser requester);

    QuotePageView listQuotes(QuoteQuery query, RequestingUser requester);

    QuoteSummaryView getSummary(RequestingUser requester);

    QuoteView getOwnedQuote(UUID id, UUID ownerId);

    QuoteView ensureSubmittable(UUID id, UUID ownerId);

    QuoteView markSubmitted(UUID id, UUID ownerId);

    QuoteView markSubmissionFailed(UUID id, UUID ownerId);
}
