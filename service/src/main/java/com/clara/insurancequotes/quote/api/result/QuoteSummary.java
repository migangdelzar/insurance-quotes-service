package com.clara.insurancequotes.quote.api.result;

import java.math.BigDecimal;
import java.util.List;

/** Stable public analytics summary for quotes visible to a requester. */
public record QuoteSummary(
        long totalQuotes,
        long draftQuotes,
        long submittedQuotes,
        long submissionFailedQuotes,
        long expiredQuotes,
        long pricedQuotes,
        BigDecimal totalMonthlyPremium,
        BigDecimal averageMonthlyPremium,
        BigDecimal submissionRate,
        List<QuoteDistribution> statusDistribution,
        List<QuoteDistribution> coverageDistribution,
        List<QuoteTrendPoint> trend) {
    public QuoteSummary {
        statusDistribution = statusDistribution == null ? null : List.copyOf(statusDistribution);
        coverageDistribution = coverageDistribution == null ? null : List.copyOf(coverageDistribution);
        trend = trend == null ? null : List.copyOf(trend);
    }
}
