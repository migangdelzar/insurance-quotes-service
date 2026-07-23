package com.clara.insurancequotes.quote.application.port.out;

import com.clara.insurancequotes.quote.domain.model.Quote;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuoteRepository {

    Quote save(Quote quote);

    Optional<Quote> findById(UUID id);

    List<Quote> findAll();

    List<UUID> findIdsToExpire(Instant cutoff);

    int markExpired(List<UUID> ids, Instant now);
}
