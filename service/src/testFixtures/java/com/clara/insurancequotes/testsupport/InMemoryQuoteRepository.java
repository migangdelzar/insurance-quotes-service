package com.clara.insurancequotes.testsupport;

import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.api.query.SortDirection;
import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import com.clara.insurancequotes.quote.application.port.out.QuoteSearchResult;
import com.clara.insurancequotes.quote.domain.model.Quote;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
    public QuoteSearchResult findPage(QuoteQuery query) {
        var filtered = store.values().stream()
                .filter(quote -> query.status() == null || quote.status() == query.status())
                .filter(quote -> query.coverage() == null || quote.coverageType() == query.coverage())
                .filter(quote -> query.search() == null || containsSearch(quote, query.search()))
                .sorted(comparatorFor(query))
                .toList();
        var from = Math.min(query.page() * query.size(), filtered.size());
        var to = Math.min(from + query.size(), filtered.size());
        return new QuoteSearchResult(filtered.subList(from, to), query.page(), query.size(), filtered.size());
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

    private static boolean containsSearch(Quote quote, String search) {
        var value = search.toLowerCase(Locale.ROOT);
        return quote.name().toLowerCase(Locale.ROOT).contains(value)
                || quote.email().toLowerCase(Locale.ROOT).contains(value);
    }

    private static Comparator<Quote> comparatorFor(QuoteQuery query) {
        Comparator<Quote> comparator =
                switch (query.sortBy()) {
                    case CREATED_AT -> Comparator.comparing(Quote::createdAt);
                    case UPDATED_AT -> Comparator.comparing(Quote::updatedAt);
                    case NAME -> Comparator.comparing(Quote::name, String.CASE_INSENSITIVE_ORDER);
                    case MONTHLY_PREMIUM -> Comparator.comparing(
                            Quote::monthlyPremium, Comparator.nullsFirst(Comparator.naturalOrder()));
                    case STATUS -> Comparator.comparing(Quote::status);
                };
        if (query.direction() == SortDirection.DESC) {
            comparator = comparator.reversed();
        }
        return comparator.thenComparing(Quote::id);
    }
}
