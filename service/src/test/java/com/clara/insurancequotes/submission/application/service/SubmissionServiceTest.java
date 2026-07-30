package com.clara.insurancequotes.submission.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clara.insurancequotes.shared.observability.BusinessMetrics;
import com.clara.insurancequotes.quote.api.result.QuoteView;
import com.clara.insurancequotes.quote.api.usecase.QuoteApi;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import com.clara.insurancequotes.submission.api.exception.InsurerUnavailableException;
import com.clara.insurancequotes.submission.application.port.out.InsurerGateway;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubmissionServiceTest {

    private final QuoteApi quoteApi = mock(QuoteApi.class);
    private final InsurerGateway insurerGateway = mock(InsurerGateway.class);
    private final SubmissionFinalizer finalizer = mock(SubmissionFinalizer.class);
    private final SubmissionService service =
            new SubmissionService(quoteApi, insurerGateway, finalizer, new BusinessMetrics(new SimpleMeterRegistry()));

    private static final UUID QUOTE_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();

    private static QuoteView viewWithStatus(QuoteStatus status) {
        return new QuoteView(
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
        when(quoteApi.getOwnedQuote(QUOTE_ID, OWNER_ID)).thenReturn(viewWithStatus(QuoteStatus.SUBMITTED));

        var view = service.submit(QUOTE_ID, OWNER_ID);

        assertThat(view.status()).isEqualTo(QuoteStatus.SUBMITTED);
        verify(insurerGateway, never()).submit(QUOTE_ID);
        verify(finalizer, never()).completeSubmission(QUOTE_ID, OWNER_ID);
    }

    @Test
    void submit_insurerAccepts_finalizes() {
        when(quoteApi.getOwnedQuote(QUOTE_ID, OWNER_ID)).thenReturn(viewWithStatus(QuoteStatus.DRAFT));
        when(quoteApi.ensureSubmittable(QUOTE_ID, OWNER_ID)).thenReturn(viewWithStatus(QuoteStatus.DRAFT));
        when(finalizer.completeSubmission(QUOTE_ID, OWNER_ID)).thenReturn(viewWithStatus(QuoteStatus.SUBMITTED));

        var view = service.submit(QUOTE_ID, OWNER_ID);

        assertThat(view.status()).isEqualTo(QuoteStatus.SUBMITTED);
        verify(insurerGateway).submit(QUOTE_ID);
    }

    @Test
    void submit_insurerFails_marksFailedAndRethrows() {
        when(quoteApi.getOwnedQuote(QUOTE_ID, OWNER_ID)).thenReturn(viewWithStatus(QuoteStatus.DRAFT));
        when(quoteApi.ensureSubmittable(QUOTE_ID, OWNER_ID)).thenReturn(viewWithStatus(QuoteStatus.DRAFT));
        doThrow(new InsurerUnavailableException("boom")).when(insurerGateway).submit(QUOTE_ID);

        assertThatThrownBy(() -> service.submit(QUOTE_ID, OWNER_ID)).isInstanceOf(InsurerUnavailableException.class);

        verify(quoteApi).markSubmissionFailed(QUOTE_ID, OWNER_ID);
        verify(finalizer, never()).completeSubmission(QUOTE_ID, OWNER_ID);
    }
}
