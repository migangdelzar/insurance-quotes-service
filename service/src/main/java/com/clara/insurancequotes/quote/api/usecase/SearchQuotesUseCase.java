package com.clara.insurancequotes.quote.api.usecase;

import com.clara.insurancequotes.quote.api.query.SearchQuotesQuery;
import com.clara.insurancequotes.quote.api.result.QuotePage;
import com.clara.insurancequotes.quote.api.type.RequestingUser;

/** Searches quotes visible to a requesting user. */
public interface SearchQuotesUseCase {

    QuotePage searchQuotes(SearchQuotesQuery query, RequestingUser requester);
}
