package com.clara.insurancequotes.shared.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Locale;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {

    private final Counter quotesCreated;
    private final Counter submissionsSucceeded;
    private final Counter submissionsFailed;
    private final Counter quotesExpired;
    private final Counter premiumCalculationsSucceeded;
    private final Counter premiumCalculationsFailed;
    private final Counter rateLimiterRedisFailures;
    private final MeterRegistry registry;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        quotesCreated = Counter.builder("quotes.created").register(registry);
        submissionsSucceeded =
                Counter.builder("quotes.submissions").tag("outcome", "success").register(registry);
        submissionsFailed =
                Counter.builder("quotes.submissions").tag("outcome", "failed").register(registry);
        quotesExpired = Counter.builder("quotes.expired").register(registry);
        premiumCalculationsSucceeded = Counter.builder("quotes.premium.calculations")
                .tag("outcome", "success")
                .register(registry);
        premiumCalculationsFailed = Counter.builder("quotes.premium.calculations")
                .tag("outcome", "failed")
                .register(registry);
        rateLimiterRedisFailures = Counter.builder("rate_limit.redis.failures").register(registry);
    }

    public void quoteCreated() {
        quotesCreated.increment();
    }

    public void submissionSucceeded() {
        submissionsSucceeded.increment();
    }

    public void submissionFailed() {
        submissionsFailed.increment();
    }

    public void quotesExpired(int count) {
        quotesExpired.increment(count);
    }

    public void coverageUpdated(String outcome, String coverageType) {
        Counter.builder("quotes.coverage.updates")
                .tag("outcome", normalizeOutcome(outcome))
                .tag("coverage_type", normalizeCoverageType(coverageType))
                .register(registry)
                .increment();
    }

    public void domainEventPublished(String eventType) {
        Counter.builder("domain.events")
                .tag("event_type", normalizeEventType(eventType))
                .register(registry)
                .increment();
    }

    public <T> T timePremiumCalculation(Supplier<T> call) {
        return time("quotes.premium.calculation", call, premiumCalculationsSucceeded, premiumCalculationsFailed);
    }

    public <T> T timeInsurerCall(Supplier<T> call) {
        return time("insurer.calls", call, null, null);
    }

    public void cacheError(String operation) {
        Counter.builder("cache.errors")
                .tag("operation", normalizeCacheOperation(operation))
                .register(registry)
                .increment();
    }

    public void rateLimitAllowed(String bucket) {
        rateLimit("allowed", bucket);
    }

    public void rateLimitRejected(String bucket) {
        rateLimit("rejected", bucket);
    }

    public void rateLimiterRedisFailure() {
        rateLimiterRedisFailures.increment();
    }

    private <T> T time(String operation, Supplier<T> call, Counter success, Counter failure) {
        var timer = Timer.builder(operation).tag("outcome", "success").register(registry);
        var sample = Timer.start(registry);
        try {
            var result = call.get();
            if (success != null) {
                success.increment();
            }
            sample.stop(timer);
            return result;
        } catch (RuntimeException exception) {
            if (failure != null) {
                failure.increment();
            }
            sample.stop(Timer.builder(operation).tag("outcome", "failed").register(registry));
            throw exception;
        }
    }

    private void rateLimit(String outcome, String bucket) {
        Counter.builder("rate_limit.requests")
                .tag("bucket", normalizeBucket(bucket))
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }

    private static String normalizeOutcome(String outcome) {
        return switch (outcome == null ? "" : outcome.toLowerCase(Locale.ROOT)) {
            case "success", "rejected", "failed" -> outcome.toLowerCase(Locale.ROOT);
            default -> "other";
        };
    }

    private static String normalizeCoverageType(String coverageType) {
        return switch (coverageType == null ? "" : coverageType.toLowerCase(Locale.ROOT)) {
            case "basic", "standard", "premium" -> coverageType.toLowerCase(Locale.ROOT);
            default -> "unknown";
        };
    }

    private static String normalizeEventType(String eventType) {
        return "quote_submitted".equalsIgnoreCase(eventType) ? "quote_submitted" : "other";
    }

    private static String normalizeCacheOperation(String operation) {
        return switch (operation == null ? "" : operation.toLowerCase(Locale.ROOT)) {
            case "get", "put", "evict", "clear" -> operation.toLowerCase(Locale.ROOT);
            default -> "other";
        };
    }

    private static String normalizeBucket(String bucket) {
        return switch (bucket == null ? "" : bucket.toLowerCase(Locale.ROOT)) {
            case "auth", "quote_mutation", "public" -> bucket.toLowerCase(Locale.ROOT);
            default -> "other";
        };
    }
}
