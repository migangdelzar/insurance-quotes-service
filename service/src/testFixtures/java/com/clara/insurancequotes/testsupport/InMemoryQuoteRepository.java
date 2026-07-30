package com.clara.insurancequotes.testsupport;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.api.query.SearchQuotesQuery;
import com.clara.insurancequotes.quote.api.query.SortDirection;
import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import com.clara.insurancequotes.quote.application.port.out.QuoteSearchResult;
import com.clara.insurancequotes.quote.application.port.out.QuoteSummaryData;
import com.clara.insurancequotes.quote.application.port.out.StaleQuoteRef;
import com.clara.insurancequotes.quote.domain.model.Quote;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
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
    public Optional<Quote> findById(UUID id, UUID ownerId) {
        return Optional.ofNullable(store.get(id))
                .filter(quote -> ownerId == null || quote.userId().equals(ownerId));
    }

    @Override
    public QuoteSearchResult findPage(SearchQuotesQuery query, UUID ownerId) {
        var filtered = store.values().stream()
                .filter(quote -> ownerId == null || quote.userId().equals(ownerId))
                .filter(quote -> query.status() == null || quote.status().name().equals(query.status().name()))
                .filter(quote -> query.coverage() == null || quote.coverageType() == query.coverage())
                .filter(quote -> query.search() == null || containsSearch(quote, query.search()))
                .sorted(comparatorFor(query))
                .toList();
        var from = Math.min(query.page() * query.size(), filtered.size());
        var to = Math.min(from + query.size(), filtered.size());
        return new QuoteSearchResult(filtered.subList(from, to), query.page(), query.size(), filtered.size());
    }

    @Override
    public QuoteSummaryData findSummary(Instant now, UUID ownerId) {
        var scoped = store.values().stream()
                .filter(quote -> ownerId == null || quote.userId().equals(ownerId))
                .toList();
        var statusCounts = new EnumMap<QuoteStatus, Long>(QuoteStatus.class);
        for (var status : QuoteStatus.values()) {
            statusCounts.put(
                    status,
                    scoped.stream().filter(quote -> quote.status() == status).count());
        }
        var coverageCounts = new EnumMap<CoverageType, Long>(CoverageType.class);
        for (var coverage : CoverageType.values()) {
            coverageCounts.put(
                    coverage,
                    scoped.stream()
                            .filter(quote -> quote.coverageType() == coverage)
                            .count());
        }
        var pricedQuotes =
                scoped.stream().filter(quote -> quote.monthlyPremium() != null).count();
        var totalPremium = scoped.stream()
                .map(Quote::monthlyPremium)
                .filter(value -> value != null)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        var averagePremium = pricedQuotes == 0
                ? java.math.BigDecimal.ZERO
                : totalPremium.divide(java.math.BigDecimal.valueOf(pricedQuotes), 2, java.math.RoundingMode.HALF_UP);

        var endDate = now.atZone(ZoneOffset.UTC).toLocalDate();
        var startDate = endDate.minusDays(6);
        var trendBuckets = new LinkedHashMap<LocalDate, long[]>();
        for (var date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            trendBuckets.put(date, new long[3]);
        }
        scoped.forEach(quote -> {
            var createdBucket =
                    trendBuckets.get(quote.createdAt().atZone(ZoneOffset.UTC).toLocalDate());
            if (createdBucket != null) {
                createdBucket[0]++;
            }
            var updatedBucket =
                    trendBuckets.get(quote.updatedAt().atZone(ZoneOffset.UTC).toLocalDate());
            if (updatedBucket != null && quote.status() == QuoteStatus.SUBMITTED) {
                updatedBucket[1]++;
            } else if (updatedBucket != null && quote.status() == QuoteStatus.SUBMISSION_FAILED) {
                updatedBucket[2]++;
            }
        });
        var trend = new ArrayList<QuoteSummaryData.TrendPoint>();
        trendBuckets.forEach(
                (date, values) -> trend.add(new QuoteSummaryData.TrendPoint(date, values[0], values[1], values[2])));
        return new QuoteSummaryData(
                scoped.size(), statusCounts, coverageCounts, pricedQuotes, totalPremium, averagePremium, trend);
    }

    @Override
    public List<StaleQuoteRef> findStaleDrafts(Instant cutoff) {
        return store.values().stream()
                .filter(quote -> quote.status().allowsExpiration())
                .filter(quote -> quote.createdAt().isBefore(cutoff))
                .map(quote -> new StaleQuoteRef(quote.id(), quote.userId()))
                .toList();
    }

    @Override
    public int markExpired(List<UUID> ids, Instant now) {
        ids.forEach(id -> findById(id, null).ifPresent(quote -> quote.expire(now)));
        return ids.size();
    }

    private static boolean containsSearch(Quote quote, String search) {
        var value = search.toLowerCase(Locale.ROOT);
        return quote.name().toLowerCase(Locale.ROOT).contains(value)
                || quote.email().toLowerCase(Locale.ROOT).contains(value);
    }

    private static Comparator<Quote> comparatorFor(SearchQuotesQuery query) {
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
