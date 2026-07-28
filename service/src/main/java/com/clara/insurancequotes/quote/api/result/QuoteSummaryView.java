package com.clara.insurancequotes.quote.api.result;

import java.math.BigDecimal;
import java.util.List;

public record QuoteSummaryView(
        long totalQuotes,
        long draftQuotes,
        long submittedQuotes,
        long submissionFailedQuotes,
        long expiredQuotes,
        long pricedQuotes,
        BigDecimal totalMonthlyPremium,
        BigDecimal averageMonthlyPremium,
        BigDecimal submissionRate,
        List<QuoteDistributionView> statusDistribution,
        List<QuoteDistributionView> coverageDistribution,
        List<QuoteTrendPointView> trend) {
    public QuoteSummaryView {
        statusDistribution = statusDistribution == null ? null : List.copyOf(statusDistribution);
        coverageDistribution = coverageDistribution == null ? null : List.copyOf(coverageDistribution);
        trend = trend == null ? null : List.copyOf(trend);
    }
}
