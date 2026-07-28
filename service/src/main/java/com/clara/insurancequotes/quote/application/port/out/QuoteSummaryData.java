package com.clara.insurancequotes.quote.application.port.out;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record QuoteSummaryData(
        long totalQuotes,
        Map<QuoteStatus, Long> statusCounts,
        Map<CoverageType, Long> coverageCounts,
        long pricedQuotes,
        BigDecimal totalMonthlyPremium,
        BigDecimal averageMonthlyPremium,
        List<TrendPoint> trend) {

    public QuoteSummaryData {
        statusCounts = Map.copyOf(statusCounts);
        coverageCounts = Map.copyOf(coverageCounts);
        totalMonthlyPremium = totalMonthlyPremium == null ? BigDecimal.ZERO : totalMonthlyPremium;
        averageMonthlyPremium = averageMonthlyPremium == null ? BigDecimal.ZERO : averageMonthlyPremium;
        trend = List.copyOf(trend);
    }

    public record TrendPoint(LocalDate date, long created, long submitted, long failed) {}
}
