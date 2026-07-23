package com.clara.insurancequotes.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {

    private final Counter quotesCreated;
    private final Counter submissionsSucceeded;
    private final Counter submissionsFailed;
    private final Counter quotesExpired;
    private final Timer insurerCall;

    public BusinessMetrics(MeterRegistry registry) {
        quotesCreated = Counter.builder("quotes.created").register(registry);
        submissionsSucceeded =
                Counter.builder("quotes.submissions").tag("outcome", "success").register(registry);
        submissionsFailed =
                Counter.builder("quotes.submissions").tag("outcome", "failed").register(registry);
        quotesExpired = Counter.builder("quotes.expired").register(registry);
        insurerCall = Timer.builder("insurer.call").register(registry);
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

    public <T> T timeInsurerCall(Supplier<T> call) {
        return insurerCall.record(call);
    }
}
