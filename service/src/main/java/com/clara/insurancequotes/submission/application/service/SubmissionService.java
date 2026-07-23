package com.clara.insurancequotes.submission.application.service;

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

    @Override
    public QuoteView submit(UUID quoteId) {
        var current = quoteApi.getQuote(quoteId);
        if (current.status().alreadySubmitted()) {
            log.debug("Ignoring duplicate submission for quote {}", quoteId);
            return current;
        }
        quoteApi.ensureSubmittable(quoteId);
        callInsurerRecordingFailure(quoteId);
        return finalizer.completeSubmission(quoteId);
    }

    private void callInsurerRecordingFailure(UUID quoteId) {
        try {
            insurerGateway.submit(quoteId);
        } catch (InsurerUnavailableException exception) {
            quoteApi.markSubmissionFailed(quoteId);
            throw exception;
        }
    }
}
