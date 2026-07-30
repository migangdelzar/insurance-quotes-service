package com.clara.insurancequotes.quote.api.usecase;

import com.clara.insurancequotes.quote.api.result.QuoteSummary;
import com.clara.insurancequotes.quote.api.type.RequestingUser;

/** Retrieves analytics for quotes visible to a requesting user. */
public interface GetQuoteSummaryUseCase {

    QuoteSummary getSummary(RequestingUser requester);
}
