package com.clara.insurancequotes.quote.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.clara.insurancequotes.pricing.api.model.CoverageType;
import com.clara.insurancequotes.pricing.api.model.Premium;
import com.clara.insurancequotes.pricing.api.port.in.PremiumCalculator;
import com.clara.insurancequotes.quote.api.model.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.model.HealthCondition;
import com.clara.insurancequotes.quote.api.model.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.application.exception.QuoteNotFoundException;
import com.clara.insurancequotes.quote.domain.exception.HealthDataNotAllowedException;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import com.clara.insurancequotes.testsupport.InMemoryQuoteRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuoteServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-22T10:00:00Z");

    private final InMemoryQuoteRepository repository = new InMemoryQuoteRepository();
    private final PremiumCalculator calculator = input -> new Premium(new BigDecimal("100.00"));
    private final QuoteService service = new QuoteService(repository, calculator, Clock.fixed(NOW, ZoneOffset.UTC));

    private static final CreateQuoteCommand ADULT = new CreateQuoteCommand("Jane Roe", "jane@example.com", 34, "06600");
    private static final CreateQuoteCommand SENIOR =
            new CreateQuoteCommand("John Elder", "john@example.com", 70, "06600");

    private static UpdateCoverageCommand plainCoverage() {
        return new UpdateCoverageCommand(CoverageType.STANDARD, null, null, null, null, null);
    }

    private static UpdateCoverageCommand seniorCoverage() {
        return new UpdateCoverageCommand(
                CoverageType.STANDARD, true, Set.of(HealthCondition.DIABETES), false, true, true);
    }

    @Test
    void create_persistsDraftAndReturnsView() {
        var view = service.create(ADULT);

        assertThat(view.status()).isEqualTo(QuoteStatus.DRAFT);
        assertThat(repository.findById(view.id())).isPresent();
    }

    @Test
    void updateCoverage_computesPremiumServerSide() {
        var id = service.create(ADULT).id();

        var view = service.updateCoverage(id, plainCoverage());

        assertThat(view.monthlyPremium()).isEqualByComparingTo("100.00");
        assertThat(view.coverageType()).isEqualTo(CoverageType.STANDARD);
    }

    @Test
    void updateCoverage_healthDataAtAge65OrBelow_isRejected() {
        var id = service.create(ADULT).id();

        assertThatThrownBy(() -> service.updateCoverage(id, seniorCoverage()))
                .isInstanceOf(HealthDataNotAllowedException.class);
    }

    @Test
    void updateCoverage_healthDataOver65_isAccepted() {
        var id = service.create(SENIOR).id();

        var view = service.updateCoverage(id, seniorCoverage());

        assertThat(view.usesTobacco()).isTrue();
        assertThat(view.conditions()).containsExactly(HealthCondition.DIABETES);
    }

    @Test
    void getQuote_unknownId_throwsNotFound() {
        assertThatThrownBy(() -> service.getQuote(UUID.randomUUID())).isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void markSubmitted_transitionsAndPersists() {
        var id = service.create(ADULT).id();
        service.updateCoverage(id, plainCoverage());

        var view = service.markSubmitted(id);

        assertThat(view.status()).isEqualTo(QuoteStatus.SUBMITTED);
        assertThat(repository.findById(id).orElseThrow().status()).isEqualTo(QuoteStatus.SUBMITTED);
    }
}
