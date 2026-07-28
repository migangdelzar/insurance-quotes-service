package com.clara.insurancequotes.quote.application.port.out;

import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.domain.model.Quote;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuoteRepository {

    Quote save(Quote quote);

    /** {@code ownerId == null} means unscoped (admin); non-null scopes to that owner. */
    Optional<Quote> findById(UUID id, UUID ownerId);

    QuoteSearchResult findPage(QuoteQuery query, UUID ownerId);

    QuoteSummaryData findSummary(Instant now, UUID ownerId);

    List<StaleQuoteRef> findStaleDrafts(Instant cutoff);

    int markExpired(List<UUID> ids, Instant now);
}
