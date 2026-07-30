package com.clara.insurancequotes.quote.api.usecase;

import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import com.clara.insurancequotes.quote.api.type.RequestingUser;
import java.util.UUID;

/** Retrieves a quote visible to a requesting user. */
public interface GetQuoteUseCase {

    QuoteDetails getQuote(UUID id, RequestingUser requester);
}
