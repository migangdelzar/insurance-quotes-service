package com.clara.insurancequotes.submission.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clara.insurancequotes.shared.observability.BusinessMetrics;
import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import com.clara.insurancequotes.quote.api.type.QuoteStatusView;
import com.clara.insurancequotes.quote.api.usecase.EnsureQuoteSubmittableUseCase;
import com.clara.insurancequotes.quote.api.usecase.GetOwnedQuoteUseCase;
import com.clara.insurancequotes.quote.api.usecase.MarkQuoteSubmissionFailedUseCase;
import com.clara.insurancequotes.submission.api.exception.InsurerUnavailableException;
import com.clara.insurancequotes.submission.application.port.out.InsurerGateway;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubmissionServiceTest {

    private final GetOwnedQuoteUseCase getOwnedQuoteUseCase = mock(GetOwnedQuoteUseCase.class);
    private final EnsureQuoteSubmittableUseCase ensureQuoteSubmittableUseCase = mock(EnsureQuoteSubmittableUseCase.class);
    private final MarkQuoteSubmissionFailedUseCase markQuoteSubmissionFailedUseCase =
            mock(MarkQuoteSubmissionFailedUseCase.class);
    private final InsurerGateway insurerGateway = mock(InsurerGateway.class);
    private final SubmissionFinalizer finalizer = mock(SubmissionFinalizer.class);
    private final SubmissionService service =
            new SubmissionService(
                    getOwnedQuoteUseCase,
                    ensureQuoteSubmittableUseCase,
                    markQuoteSubmissionFailedUseCase,
                    insurerGateway,
                    finalizer,
                    new BusinessMetrics(new SimpleMeterRegistry()));

    private static final UUID QUOTE_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();

    private static QuoteDetails viewWithStatus(QuoteStatusView status) {
        return new QuoteDetails(
                QUOTE_ID,
                "Jane Roe",
                "jane@example.com",
                34,
                "06600",
                null,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("100.00"),
                status,
                Instant.now(),
                Instant.now());
    }

    @Test
    void submit_alreadySubmitted_isIdempotentAndSkipsInsurer() {
        when(getOwnedQuoteUseCase.getOwnedQuote(QUOTE_ID, OWNER_ID)).thenReturn(viewWithStatus(QuoteStatusView.SUBMITTED));

        var view = service.submit(QUOTE_ID, OWNER_ID);

        assertThat(view.status()).isEqualTo(QuoteStatusView.SUBMITTED);
        verify(insurerGateway, never()).submit(QUOTE_ID);
        verify(finalizer, never()).completeSubmission(QUOTE_ID, OWNER_ID);
    }

    @Test
    void submit_insurerAccepts_finalizes() {
        when(getOwnedQuoteUseCase.getOwnedQuote(QUOTE_ID, OWNER_ID)).thenReturn(viewWithStatus(QuoteStatusView.DRAFT));
        when(ensureQuoteSubmittableUseCase.ensureSubmittable(QUOTE_ID, OWNER_ID))
                .thenReturn(viewWithStatus(QuoteStatusView.DRAFT));
        when(finalizer.completeSubmission(QUOTE_ID, OWNER_ID)).thenReturn(viewWithStatus(QuoteStatusView.SUBMITTED));

        var view = service.submit(QUOTE_ID, OWNER_ID);

        assertThat(view.status()).isEqualTo(QuoteStatusView.SUBMITTED);
        verify(insurerGateway).submit(QUOTE_ID);
    }

    @Test
    void submit_insurerFails_marksFailedAndRethrows() {
        when(getOwnedQuoteUseCase.getOwnedQuote(QUOTE_ID, OWNER_ID)).thenReturn(viewWithStatus(QuoteStatusView.DRAFT));
        when(ensureQuoteSubmittableUseCase.ensureSubmittable(QUOTE_ID, OWNER_ID))
                .thenReturn(viewWithStatus(QuoteStatusView.DRAFT));
        doThrow(new InsurerUnavailableException("boom")).when(insurerGateway).submit(QUOTE_ID);

        assertThatThrownBy(() -> service.submit(QUOTE_ID, OWNER_ID)).isInstanceOf(InsurerUnavailableException.class);

        verify(markQuoteSubmissionFailedUseCase).markSubmissionFailed(QUOTE_ID, OWNER_ID);
        verify(finalizer, never()).completeSubmission(QUOTE_ID, OWNER_ID);
    }
}
