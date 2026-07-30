package com.clara.insurancequotes.quote.application.service;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.api.result.QuoteDistribution;
import com.clara.insurancequotes.quote.api.result.QuoteSummary;
import com.clara.insurancequotes.quote.api.result.QuoteTrendPoint;
import com.clara.insurancequotes.quote.api.usecase.GetQuoteSummaryUseCase;
import com.clara.insurancequotes.quote.api.type.RequestingUser;
import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetQuoteSummaryService implements GetQuoteSummaryUseCase {

    private final QuoteRepository repository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public QuoteSummary getSummary(RequestingUser requester) {
        var data = repository.findSummary(clock.instant(), requester.admin() ? null : requester.id());
        var submitted = data.statusCounts().getOrDefault(QuoteStatus.SUBMITTED, 0L);
        var failed = data.statusCounts().getOrDefault(QuoteStatus.SUBMISSION_FAILED, 0L);
        var attempts = submitted + failed;
        var submissionRate = attempts == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(submitted)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(attempts), 2, RoundingMode.HALF_UP);
        var statusDistribution = Arrays.stream(QuoteStatus.values())
                .map(status -> new QuoteDistribution(status.name(), data.statusCounts().getOrDefault(status, 0L)))
                .toList();
        var coverageDistribution = Arrays.stream(CoverageType.values())
                .map(coverage -> new QuoteDistribution(coverage.name(), data.coverageCounts().getOrDefault(coverage, 0L)))
                .toList();
        var trend = data.trend().stream()
                .map(point -> new QuoteTrendPoint(point.date(), point.created(), point.submitted(), point.failed()))
                .toList();
        return new QuoteSummary(
                data.totalQuotes(),
                data.statusCounts().getOrDefault(QuoteStatus.DRAFT, 0L),
                submitted,
                failed,
                data.statusCounts().getOrDefault(QuoteStatus.EXPIRED, 0L),
                data.pricedQuotes(),
                data.totalMonthlyPremium(),
                data.averageMonthlyPremium(),
                submissionRate,
                statusDistribution,
                coverageDistribution,
                trend);
    }
}
