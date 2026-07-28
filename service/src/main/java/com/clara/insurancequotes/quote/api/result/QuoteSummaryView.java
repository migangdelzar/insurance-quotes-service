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
        List<QuoteTrendPointView> trend) {}
