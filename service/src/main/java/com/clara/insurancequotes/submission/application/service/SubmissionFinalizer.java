package com.clara.insurancequotes.submission.application.service;

import com.clara.insurancequotes.quote.api.result.QuoteView;
import com.clara.insurancequotes.quote.api.usecase.QuoteApi;
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

    private final QuoteApi quoteApi;
    private final ApplicationEventPublisher events;

    @Transactional
    public QuoteView completeSubmission(UUID quoteId) {
        var view = quoteApi.markSubmitted(quoteId);
        events.publishEvent(new QuoteSubmitted(view.id(), view.monthlyPremium(), view.updatedAt()));
        log.debug("Finalized quote submission {}", quoteId);
        return view;
    }
}
