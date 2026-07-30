package com.clara.insurancequotes.submission.application.service;

import com.clara.insurancequotes.shared.observability.BusinessMetrics;
import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import com.clara.insurancequotes.quote.api.usecase.MarkQuoteSubmittedUseCase;
import com.clara.insurancequotes.submission.api.event.QuoteSubmitted;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Commits the final quote state and its durable outbox event atomically. */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionFinalizer {

    private final MarkQuoteSubmittedUseCase markQuoteSubmittedUseCase;
    private final ApplicationEventPublisher events;
    private final BusinessMetrics metrics;

    @Transactional
    public QuoteDetails completeSubmission(UUID quoteId, UUID ownerId) {
        var view = markQuoteSubmittedUseCase.markSubmitted(quoteId, ownerId);
        events.publishEvent(new QuoteSubmitted(view.id(), view.monthlyPremium(), view.updatedAt()));
        metrics.domainEventPublished("quote_submitted");
        log.debug("Finalized quote submission {}", quoteId);
        return view;
    }
}
