package com.clara.insurancequotes.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BusinessMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final BusinessMetrics metrics = new BusinessMetrics(registry);

    @Test
    void recordsQuoteLifecycleCountersWithBoundedDimensions() {
        metrics.quoteCreated();
        metrics.coverageUpdated("success", "STANDARD");
        metrics.coverageUpdated("rejected", "STANDARD");
        metrics.submissionSucceeded();
        metrics.submissionFailed();
        metrics.domainEventPublished("quote_submitted");

        assertThat(registry.get("quotes.created").counter().count()).isEqualTo(1);
        assertThat(registry.get("quotes.coverage.updates")
                        .tag("outcome", "success")
                        .tag("coverage_type", "standard")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.get("quotes.coverage.updates")
                        .tag("outcome", "rejected")
                        .tag("coverage_type", "standard")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.get("quotes.submissions")
                        .tag("outcome", "success")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.get("quotes.submissions")
                        .tag("outcome", "failed")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.get("domain.events")
                        .tag("event_type", "quote_submitted")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void recordsPremiumAndInsurerTimingWithSuccessAndFailureOutcomes() {
        metrics.timePremiumCalculation(() -> new BigDecimal("100.00"));
        metrics.timeInsurerCall(() -> null);

        assertThat(registry.get("quotes.premium.calculations")
                        .tag("outcome", "success")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.get("quotes.premium.calculation")
                        .tag("outcome", "success")
                        .timer()
                        .count())
                .isEqualTo(1);
        assertThat(registry.get("insurer.calls")
                        .tag("outcome", "success")
                        .timer()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void recordsOperationalFailuresAndRateLimitOutcomes() {
        metrics.cacheError("get");
        metrics.rateLimitAllowed("quote_mutation");
        metrics.rateLimitRejected("auth");
        metrics.rateLimiterRedisFailure();

        assertThat(registry.get("cache.errors")
                        .tag("operation", "get")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.get("rate_limit.requests")
                        .tag("bucket", "quote_mutation")
                        .tag("outcome", "allowed")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.get("rate_limit.requests")
                        .tag("bucket", "auth")
                        .tag("outcome", "rejected")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.get("rate_limit.redis.failures").counter().count()).isEqualTo(1);
    }
}
