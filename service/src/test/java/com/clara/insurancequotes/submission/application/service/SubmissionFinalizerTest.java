package com.clara.insurancequotes.submission.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clara.insurancequotes.shared.observability.BusinessMetrics;
import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import com.clara.insurancequotes.quote.api.usecase.MarkQuoteSubmittedUseCase;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class SubmissionFinalizerTest {

    private static final UUID QUOTE_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();

    private final MarkQuoteSubmittedUseCase markQuoteSubmittedUseCase = mock(MarkQuoteSubmittedUseCase.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final SubmissionFinalizer finalizer =
            new SubmissionFinalizer(markQuoteSubmittedUseCase, events, new BusinessMetrics(registry));

    @Test
    void completeSubmission_publishesDurableBusinessEventAndRecordsMetric() {
        var view = mock(QuoteDetails.class);
        when(view.id()).thenReturn(QUOTE_ID);
        when(view.monthlyPremium()).thenReturn(null);
        when(view.updatedAt()).thenReturn(Instant.parse("2026-07-26T08:00:00Z"));
        when(markQuoteSubmittedUseCase.markSubmitted(QUOTE_ID, OWNER_ID)).thenReturn(view);

        finalizer.completeSubmission(QUOTE_ID, OWNER_ID);

        verify(events).publishEvent(org.mockito.ArgumentMatchers.<Object>any());
        assertThat(registry.get("domain.events")
                        .tag("event_type", "quote_submitted")
                        .counter()
                        .count())
                .isEqualTo(1);
    }
}
