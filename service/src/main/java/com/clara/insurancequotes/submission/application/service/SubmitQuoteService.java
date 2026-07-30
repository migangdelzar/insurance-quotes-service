package com.clara.insurancequotes.submission.application.service;

import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import com.clara.insurancequotes.quote.api.type.QuoteStatusView;
import com.clara.insurancequotes.quote.api.usecase.EnsureQuoteSubmittableUseCase;
import com.clara.insurancequotes.quote.api.usecase.GetOwnedQuoteUseCase;
import com.clara.insurancequotes.quote.api.usecase.MarkQuoteSubmissionFailedUseCase;
import com.clara.insurancequotes.shared.observability.BusinessMetrics;
import com.clara.insurancequotes.submission.api.exception.InsurerUnavailableException;
import com.clara.insurancequotes.submission.api.usecase.SubmitQuoteUseCase;
import com.clara.insurancequotes.submission.application.port.out.InsurerSubmissionPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Orchestrates the external insurer call without holding a database transaction open. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubmitQuoteService implements SubmitQuoteUseCase {

    private final GetOwnedQuoteUseCase getOwnedQuoteUseCase;
    private final EnsureQuoteSubmittableUseCase ensureQuoteSubmittableUseCase;
    private final MarkQuoteSubmissionFailedUseCase markQuoteSubmissionFailedUseCase;
    private final InsurerSubmissionPort insurerSubmissionPort;
    private final FinalizeQuoteSubmissionService finalizer;
    private final BusinessMetrics metrics;

    @Override
    public QuoteDetails submit(UUID quoteId, UUID ownerId) {
        var current = getOwnedQuoteUseCase.getOwnedQuote(quoteId, ownerId);
        if (current.status() == QuoteStatusView.SUBMITTED) {
            log.debug("Ignoring duplicate submission for quote {}", quoteId);
            return current;
        }
        ensureQuoteSubmittableUseCase.ensureSubmittable(quoteId, ownerId);
        callInsurerRecordingFailure(quoteId, ownerId);
        var completed = finalizer.completeSubmission(quoteId, ownerId);
        metrics.submissionSucceeded();
        return completed;
    }

    private void callInsurerRecordingFailure(UUID quoteId, UUID ownerId) {
        try {
            metrics.timeInsurerCall(() -> {
                insurerSubmissionPort.submit(quoteId);
                return null;
            });
        } catch (InsurerUnavailableException exception) {
            metrics.submissionFailed();
            markQuoteSubmissionFailedUseCase.markSubmissionFailed(quoteId, ownerId);
            throw exception;
        }
    }
}
