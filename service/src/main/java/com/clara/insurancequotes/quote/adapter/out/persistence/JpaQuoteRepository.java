package com.clara.insurancequotes.quote.adapter.out.persistence;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.api.query.SortDirection;
import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import com.clara.insurancequotes.quote.application.port.out.QuoteSearchResult;
import com.clara.insurancequotes.quote.application.port.out.QuoteSummaryData;
import com.clara.insurancequotes.quote.domain.model.Quote;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    public QuoteSearchResult findPage(QuoteQuery query) {
        Specification<Quote> specification = (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (query.status() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), query.status()));
        }
        if (query.coverage() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("coverageType"), query.coverage()));
        }
        if (query.search() != null) {
            var pattern = "%" + query.search().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern)));
        }
        var direction = query.direction() == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        var sort = Sort.by(direction, query.sortBy().property()).and(Sort.by(Sort.Direction.ASC, "id"));
        var page = delegate.findAll(specification, PageRequest.of(query.page(), query.size(), sort));
        return new QuoteSearchResult(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public QuoteSummaryData findSummary(Instant now) {
        var endDate = now.atZone(ZoneOffset.UTC).toLocalDate();
        var startDate = endDate.minusDays(6);
        var statusCounts = new EnumMap<QuoteStatus, Long>(QuoteStatus.class);
        for (var status : QuoteStatus.values()) {
            statusCounts.put(status, delegate.countByStatus(status));
        }
        var coverageCounts = new EnumMap<CoverageType, Long>(CoverageType.class);
        for (var coverage : CoverageType.values()) {
            coverageCounts.put(coverage, delegate.countByCoverageType(coverage));
        }
        var trendBuckets = new LinkedHashMap<LocalDate, long[]>();
        for (var date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            trendBuckets.put(date, new long[3]);
        }
        var trendStart = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        for (var row : delegate.findTrendRows(trendStart, now)) {
            var createdAt = (Instant) row[0];
            var updatedAt = (Instant) row[1];
            var status = (QuoteStatus) row[2];
            var createdDate = createdAt.atZone(ZoneOffset.UTC).toLocalDate();
            var createdBucket = trendBuckets.get(createdDate);
            if (createdBucket != null) {
                createdBucket[0]++;
            }
            var updatedDate = updatedAt.atZone(ZoneOffset.UTC).toLocalDate();
            var updatedBucket = trendBuckets.get(updatedDate);
            if (updatedBucket != null && status == QuoteStatus.SUBMITTED) {
                updatedBucket[1]++;
            } else if (updatedBucket != null && status == QuoteStatus.SUBMISSION_FAILED) {
                updatedBucket[2]++;
            }
        }
        var trend = new ArrayList<QuoteSummaryData.TrendPoint>();
        trendBuckets.forEach(
                (date, values) -> trend.add(new QuoteSummaryData.TrendPoint(date, values[0], values[1], values[2])));
        return new QuoteSummaryData(
                delegate.count(),
                statusCounts,
                coverageCounts,
                delegate.countByMonthlyPremiumIsNotNull(),
                delegate.sumMonthlyPremium(),
                delegate.averageMonthlyPremium(),
                trend);
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
