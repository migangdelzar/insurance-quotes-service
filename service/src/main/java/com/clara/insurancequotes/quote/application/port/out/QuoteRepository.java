package com.clara.insurancequotes.quote.application.port.out;

import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.domain.model.Quote;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuoteRepository {

    Quote save(Quote quote);

    Optional<Quote> findById(UUID id);

    QuoteSearchResult findPage(QuoteQuery query);

    QuoteSummaryData findSummary(Instant now);

    List<UUID> findIdsToExpire(Instant cutoff);

    int markExpired(List<UUID> ids, Instant now);
}
