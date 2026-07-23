package com.clara.insurancequotes.testsupport;

import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import com.clara.insurancequotes.quote.domain.model.Quote;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryQuoteRepository implements QuoteRepository {

    private final Map<UUID, Quote> store = new ConcurrentHashMap<>();

    @Override
    public Quote save(Quote quote) {
        store.put(quote.id(), quote);
        return quote;
    }

    @Override
    public Optional<Quote> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Quote> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public List<UUID> findIdsToExpire(Instant cutoff) {
        return store.values().stream()
                .filter(quote -> quote.status().allowsExpiration())
                .filter(quote -> quote.createdAt().isBefore(cutoff))
                .map(Quote::id)
                .toList();
    }

    @Override
    public int markExpired(List<UUID> ids, Instant now) {
        ids.forEach(id -> findById(id).ifPresent(quote -> quote.expire(now)));
        return ids.size();
    }
}
