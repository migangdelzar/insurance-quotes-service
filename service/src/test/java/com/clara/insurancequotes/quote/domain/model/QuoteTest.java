package com.clara.insurancequotes.quote.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.clara.insurancequotes.pricing.api.CoverageType;
import com.clara.insurancequotes.quote.QuoteMother;
import com.clara.insurancequotes.quote.api.IncompleteQuoteException;
import com.clara.insurancequotes.quote.api.InvalidStateTransitionException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class QuoteTest {

    private static final Instant NOW = Instant.parse("2026-07-22T10:00:00Z");

    @Test
    void createDraft_startsInDraftWithoutCoverage() {
        var quote = QuoteMother.draft();

        assertThat(quote.status()).isEqualTo(QuoteStatus.DRAFT);
        assertThat(quote.coverageType()).isNull();
        assertThat(quote.monthlyPremium()).isNull();
    }

    @Test
    void updateCoverage_onDraft_setsCoverageAndPremium() {
        var quote = QuoteMother.draft();

        quote.updateCoverage(CoverageType.STANDARD, HealthProfile.none(), new BigDecimal("100.00"), NOW);

        assertThat(quote.coverageType()).isEqualTo(CoverageType.STANDARD);
        assertThat(quote.monthlyPremium()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(quote.status()).isEqualTo(QuoteStatus.DRAFT);
    }

    @Test
    void markSubmitted_fromDraftWithCoverage_transitionsToSubmitted() {
        var quote = QuoteMother.submittableDraft();

        quote.ensureSubmittable();
        quote.markSubmitted(NOW);

        assertThat(quote.status()).isEqualTo(QuoteStatus.SUBMITTED);
    }

    @Test
    void ensureSubmittable_withoutCoverage_throwsIncomplete() {
        var quote = QuoteMother.draft();

        assertThatThrownBy(quote::ensureSubmittable).isInstanceOf(IncompleteQuoteException.class);
    }

    @Test
    void markSubmissionFailed_thenSubmittable_again() {
        var quote = QuoteMother.submittableDraft();
        quote.markSubmissionFailed(NOW);

        assertThat(quote.status()).isEqualTo(QuoteStatus.SUBMISSION_FAILED);
        quote.ensureSubmittable();
    }

    @Test
    void expiredQuote_isNeverSubmittable() {
        var quote = QuoteMother.draft();
        quote.expire(NOW);

        assertThat(quote.status()).isEqualTo(QuoteStatus.EXPIRED);
        assertThatThrownBy(quote::ensureSubmittable).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void updateCoverage_onSubmittedQuote_throwsInvalidTransition() {
        var quote = QuoteMother.submittableDraft();
        quote.markSubmitted(NOW);

        assertThatThrownBy(() ->
                        quote.updateCoverage(CoverageType.BASIC, HealthProfile.none(), new BigDecimal("50.00"), NOW))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void statusFlags_areDataDriven() {
        assertThat(QuoteStatus.DRAFT.allowsSubmission()).isTrue();
        assertThat(QuoteStatus.SUBMISSION_FAILED.allowsSubmission()).isTrue();
        assertThat(QuoteStatus.SUBMITTED.allowsSubmission()).isFalse();
        assertThat(QuoteStatus.EXPIRED.allowsSubmission()).isFalse();
        assertThat(QuoteStatus.SUBMITTED.alreadySubmitted()).isTrue();
        assertThat(QuoteStatus.DRAFT.allowsExpiration()).isTrue();
        assertThat(QuoteStatus.SUBMISSION_FAILED.allowsExpiration()).isFalse();
    }
}
