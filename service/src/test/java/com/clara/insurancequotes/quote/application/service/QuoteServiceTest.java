package com.clara.insurancequotes.quote.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.clara.insurancequotes.pricing.api.result.Premium;
import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.pricing.api.usecase.CalculatePremiumUseCase;
import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.api.type.HealthCondition;
import com.clara.insurancequotes.quote.api.usecase.RequestingUser;
import com.clara.insurancequotes.quote.application.exception.QuoteNotFoundException;
import com.clara.insurancequotes.quote.domain.exception.HealthDataNotAllowedException;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import com.clara.insurancequotes.shared.observability.BusinessMetrics;
import com.clara.insurancequotes.testsupport.InMemoryQuoteRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuoteServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-22T10:00:00Z");
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID OTHER_OWNER = UUID.randomUUID();
    private static final RequestingUser AS_OWNER = new RequestingUser(OWNER, false);
    private static final RequestingUser AS_ADMIN = new RequestingUser(UUID.randomUUID(), true);

    private final InMemoryQuoteRepository repository = new InMemoryQuoteRepository();
    private final CalculatePremiumUseCase calculator = input -> new Premium(new BigDecimal("100.00"));
    private final SimpleMeterRegistry metricsRegistry = new SimpleMeterRegistry();
    private final QuoteService service = new QuoteService(
            repository, calculator, Clock.fixed(NOW, ZoneOffset.UTC), new BusinessMetrics(metricsRegistry));

    private static final CreateQuoteCommand ADULT = new CreateQuoteCommand("Jane Roe", "jane@example.com", 34, "06600");
    private static final CreateQuoteCommand SENIOR =
            new CreateQuoteCommand("John Elder", "john@example.com", 70, "06600");

    private static UpdateCoverageCommand plainCoverage() {
        return new UpdateCoverageCommand(CoverageType.STANDARD, null, null, null, null, null);
    }

    private static UpdateCoverageCommand seniorCoverage() {
        return new UpdateCoverageCommand(
                CoverageType.STANDARD,
                true,
                Set.of(HealthCondition.DIABETES, HealthCondition.HYPERTENSION),
                false,
                true,
                true);
    }

    @Test
    void create_persistsDraftAndReturnsView() {
        var view = service.create(ADULT, OWNER);

        assertThat(view.status()).isEqualTo(QuoteStatus.DRAFT);
        assertThat(repository.findById(view.id(), OWNER)).isPresent();
    }

    @Test
    void updateCoverage_computesPremiumServerSide() {
        var id = service.create(ADULT, OWNER).id();

        var view = service.updateCoverage(id, plainCoverage(), OWNER);

        assertThat(view.monthlyPremium()).isEqualByComparingTo("100.00");
        assertThat(view.coverageType()).isEqualTo(CoverageType.STANDARD);
        assertThat(metricsRegistry
                        .get("quotes.coverage.updates")
                        .tag("outcome", "success")
                        .tag("coverage_type", "standard")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void updateCoverage_healthDataAtAge65OrBelow_isRejected() {
        var id = service.create(ADULT, OWNER).id();

        assertThatThrownBy(() -> service.updateCoverage(id, seniorCoverage(), OWNER))
                .isInstanceOf(HealthDataNotAllowedException.class);
        assertThat(metricsRegistry
                        .get("quotes.coverage.updates")
                        .tag("outcome", "rejected")
                        .tag("coverage_type", "standard")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void updateCoverage_healthDataOver65_isAccepted() {
        var id = service.create(SENIOR, OWNER).id();

        var view = service.updateCoverage(id, seniorCoverage(), OWNER);

        assertThat(view.usesTobacco()).isTrue();
        assertThat(view.conditions()).containsExactlyInAnyOrder(HealthCondition.DIABETES, HealthCondition.HYPERTENSION);
    }

    @Test
    void getQuote_unknownId_throwsNotFound() {
        assertThatThrownBy(() -> service.getQuote(UUID.randomUUID(), AS_OWNER))
                .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void getQuote_ownedByOtherUser_throwsNotFound() {
        var id = service.create(ADULT, OTHER_OWNER).id();

        assertThatThrownBy(() -> service.getQuote(id, AS_OWNER)).isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void getQuote_asAdmin_seesAnyUsersQuote() {
        var id = service.create(ADULT, OTHER_OWNER).id();

        var view = service.getQuote(id, AS_ADMIN);

        assertThat(view.id()).isEqualTo(id);
    }

    @Test
    void updateCoverage_ownedByOtherUser_throwsNotFoundEvenForAdmin() {
        var id = service.create(ADULT, OTHER_OWNER).id();
        var adminOwnId = AS_ADMIN.id();

        assertThatThrownBy(() -> service.updateCoverage(id, plainCoverage(), adminOwnId))
                .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void markSubmitted_transitionsAndPersists() {
        var id = service.create(ADULT, OWNER).id();
        service.updateCoverage(id, plainCoverage(), OWNER);

        var view = service.markSubmitted(id, OWNER);

        assertThat(view.status()).isEqualTo(QuoteStatus.SUBMITTED);
        assertThat(repository.findById(id, OWNER).orElseThrow().status()).isEqualTo(QuoteStatus.SUBMITTED);
    }

    @Test
    void listQuotes_returnsFilteredOrderedPageMetadata() {
        var jane = service.create(ADULT, OWNER);
        service.updateCoverage(jane.id(), plainCoverage(), OWNER);
        service.markSubmitted(jane.id(), OWNER);
        service.create(SENIOR, OWNER);

        var result = service.listQuotes(QuoteQuery.of(0, 1, "jane", "SUBMITTED", "STANDARD", "name", "asc"), AS_OWNER);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("Jane Roe");
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void listQuotes_excludesOtherUsersQuotesForNonAdmin() {
        service.create(ADULT, OWNER);
        service.create(SENIOR, OTHER_OWNER);

        var result = service.listQuotes(QuoteQuery.defaults(), AS_OWNER);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void listQuotes_includesEveryUsersQuotesForAdmin() {
        service.create(ADULT, OWNER);
        service.create(SENIOR, OTHER_OWNER);

        var result = service.listQuotes(QuoteQuery.defaults(), AS_ADMIN);

        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void getSummary_returnsAggregateMetricsAndSevenDayTrend() {
        var draft = service.create(ADULT, OWNER);
        var submitted = service.create(SENIOR, OWNER);
        service.updateCoverage(submitted.id(), plainCoverage(), OWNER);
        service.markSubmitted(submitted.id(), OWNER);
        var failed = service.create(new CreateQuoteCommand("Failed Quote", "failed@example.com", 40, "06600"), OWNER);
        service.markSubmissionFailed(failed.id(), OWNER);

        var result = service.getSummary(AS_OWNER);

        assertThat(result.totalQuotes()).isEqualTo(3);
        assertThat(result.draftQuotes()).isEqualTo(1);
        assertThat(result.submittedQuotes()).isEqualTo(1);
        assertThat(result.submissionFailedQuotes()).isEqualTo(1);
        assertThat(result.expiredQuotes()).isZero();
        assertThat(result.pricedQuotes()).isEqualTo(1);
        assertThat(result.totalMonthlyPremium()).isEqualByComparingTo("100.00");
        assertThat(result.averageMonthlyPremium()).isEqualByComparingTo("100.00");
        assertThat(result.submissionRate()).isEqualByComparingTo("50.00");
        assertThat(result.statusDistribution())
                .extracting("key")
                .containsExactly("DRAFT", "SUBMITTED", "SUBMISSION_FAILED", "EXPIRED");
        assertThat(result.coverageDistribution()).extracting("key").containsExactly("BASIC", "STANDARD", "PREMIUM");
        assertThat(result.trend()).hasSize(7);
        assertThat(result.trend().get(6).created()).isEqualTo(3);
        assertThat(result.trend().get(6).submitted()).isEqualTo(1);
        assertThat(result.trend().get(6).failed()).isEqualTo(1);
        assertThat(draft.status()).isEqualTo(QuoteStatus.DRAFT);
    }
}
