package com.clara.insurancequotes.submission.application.service;

import com.clara.insurancequotes.config.BusinessMetrics;
import com.clara.insurancequotes.quote.api.result.QuoteView;
import com.clara.insurancequotes.quote.api.usecase.QuoteApi;
import com.clara.insurancequotes.submission.api.exception.InsurerUnavailableException;
import com.clara.insurancequotes.submission.api.usecase.SubmissionApi;
import com.clara.insurancequotes.submission.application.port.out.InsurerGateway;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Orchestrates the external insurer call without holding a database transaction open. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService implements SubmissionApi {

    private final QuoteApi quoteApi;
    private final InsurerGateway insurerGateway;
    private final SubmissionFinalizer finalizer;
    private final BusinessMetrics metrics;

    @Override
    public QuoteView submit(UUID quoteId) {
        var current = quoteApi.getQuote(quoteId);
        if (current.status().alreadySubmitted()) {
            log.debug("Ignoring duplicate submission for quote {}", quoteId);
            return current;
        }
        quoteApi.ensureSubmittable(quoteId);
        callInsurerRecordingFailure(quoteId);
        var completed = finalizer.completeSubmission(quoteId);
        metrics.submissionSucceeded();
        return completed;
    }

    private void callInsurerRecordingFailure(UUID quoteId) {
        try {
            metrics.timeInsurerCall(() -> {
                insurerGateway.submit(quoteId);
                return null;
            });
        } catch (InsurerUnavailableException exception) {
            metrics.submissionFailed();
            quoteApi.markSubmissionFailed(quoteId);
            throw exception;
        }
    }
}
