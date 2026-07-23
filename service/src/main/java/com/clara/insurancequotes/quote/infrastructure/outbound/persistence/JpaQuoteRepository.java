package com.clara.insurancequotes.quote.infrastructure.outbound.persistence;

import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import com.clara.insurancequotes.quote.domain.model.Quote;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class JpaQuoteRepository implements QuoteRepository {

    private final SpringDataQuoteRepository delegate;

    @Override
    public Quote save(Quote quote) {
        return delegate.save(quote);
    }

    @Override
    public Optional<Quote> findById(UUID id) {
        return delegate.findById(id);
    }

    @Override
    public List<Quote> findAll() {
        return delegate.findAll();
    }

    @Override
    public List<UUID> findIdsToExpire(Instant cutoff) {
        return delegate.findIdsToExpire(cutoff);
    }

    @Override
    public int markExpired(List<UUID> ids, Instant now) {
        var updated = delegate.markExpired(ids, now);
        log.debug("Marked {} quotes as expired", updated);
        return updated;
    }
}
