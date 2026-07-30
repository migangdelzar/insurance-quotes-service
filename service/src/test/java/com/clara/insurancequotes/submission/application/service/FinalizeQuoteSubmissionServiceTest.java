package com.clara.insurancequotes.submission.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import com.clara.insurancequotes.quote.api.usecase.MarkQuoteSubmittedUseCase;
import com.clara.insurancequotes.shared.observability.BusinessMetrics;
import com.clara.insurancequotes.submission.api.event.QuoteSubmitted;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class FinalizeQuoteSubmissionServiceTest {

    private static final UUID QUOTE_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();

    private final MarkQuoteSubmittedUseCase markQuoteSubmittedUseCase = mock(MarkQuoteSubmittedUseCase.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final FinalizeQuoteSubmissionService finalizer =
            new FinalizeQuoteSubmissionService(markQuoteSubmittedUseCase, events, new BusinessMetrics(registry));

    @Test
    void completeSubmission_publishesDurableBusinessEventAndRecordsMetric() {
        var view = mock(QuoteDetails.class);
        when(view.id()).thenReturn(QUOTE_ID);
        when(view.monthlyPremium()).thenReturn(null);
        when(view.updatedAt()).thenReturn(Instant.parse("2026-07-26T08:00:00Z"));
        when(markQuoteSubmittedUseCase.markSubmitted(QUOTE_ID, OWNER_ID)).thenReturn(view);

        finalizer.completeSubmission(QUOTE_ID, OWNER_ID);

        var eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(QuoteSubmitted.class);
        var event = (QuoteSubmitted) eventCaptor.getValue();
        assertThat(event.quoteId()).isEqualTo(QUOTE_ID);
        assertThat(event.monthlyPremium()).isNull();
        assertThat(event.submittedAt()).isEqualTo(view.updatedAt());
        assertThat(registry.get("domain.events")
                        .tag("event_type", "quote_submitted")
                        .counter()
                        .count())
                .isEqualTo(1);
    }
}
