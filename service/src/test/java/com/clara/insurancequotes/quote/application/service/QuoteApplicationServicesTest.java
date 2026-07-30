package com.clara.insurancequotes.quote.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.clara.insurancequotes.pricing.api.result.Premium;
import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.pricing.api.usecase.CalculatePremiumUseCase;
import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.exception.QuoteNotFoundException;
import com.clara.insurancequotes.quote.api.query.SearchQuotesQuery;
import com.clara.insurancequotes.quote.api.type.HealthCondition;
import com.clara.insurancequotes.quote.api.type.QuoteStatusView;
import com.clara.insurancequotes.quote.api.type.RequestingUser;
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

class QuoteApplicationServicesTest {

    private static final Instant NOW = Instant.parse("2026-07-22T10:00:00Z");
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID OTHER_OWNER = UUID.randomUUID();
    private static final RequestingUser AS_OWNER = new RequestingUser(OWNER, false);
    private static final RequestingUser AS_ADMIN = new RequestingUser(UUID.randomUUID(), true);

    private final InMemoryQuoteRepository repository = new InMemoryQuoteRepository();
    private final CalculatePremiumUseCase calculator = input -> new Premium(new BigDecimal("100.00"));
    private final SimpleMeterRegistry metricsRegistry = new SimpleMeterRegistry();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final BusinessMetrics metrics = new BusinessMetrics(metricsRegistry);
    private final CreateQuoteService createQuoteService = new CreateQuoteService(repository, clock, metrics);
    private final UpdateCoverageService updateCoverageService =
            new UpdateCoverageService(repository, calculator, clock, metrics);
    private final GetQuoteService getQuoteService = new GetQuoteService(repository);
    private final SearchQuotesService searchQuotesService = new SearchQuotesService(repository);
    private final GetQuoteSummaryService getQuoteSummaryService = new GetQuoteSummaryService(repository, clock);
    private final MarkQuoteSubmittedService markQuoteSubmittedService = new MarkQuoteSubmittedService(repository, clock);
    private final MarkQuoteSubmissionFailedService markQuoteSubmissionFailedService =
            new MarkQuoteSubmissionFailedService(repository, clock);

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
    void createQuoteService_persistsDraftAndReturnsDetails() {
        var details = createQuoteService.create(ADULT, OWNER);

        assertThat(details.status()).isEqualTo(QuoteStatusView.DRAFT);
        assertThat(repository.findById(details.id(), OWNER)).isPresent();
    }

    @Test
    void updateCoverageService_computesPremiumServerSide() {
        var id = createQuoteService.create(ADULT, OWNER).id();

        var details = updateCoverageService.updateCoverage(id, plainCoverage(), OWNER);

        assertThat(details.monthlyPremium()).isEqualByComparingTo("100.00");
        assertThat(details.coverageType()).isEqualTo(CoverageType.STANDARD);
        assertThat(metricsRegistry
                        .get("quotes.coverage.updates")
                        .tag("outcome", "success")
                        .tag("coverage_type", "standard")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void updateCoverageService_rejectsHealthDataAtAge65OrBelow() {
        var id = createQuoteService.create(ADULT, OWNER).id();

        assertThatThrownBy(() -> updateCoverageService.updateCoverage(id, seniorCoverage(), OWNER))
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
    void updateCoverageService_acceptsHealthDataOver65() {
        var id = createQuoteService.create(SENIOR, OWNER).id();

        var details = updateCoverageService.updateCoverage(id, seniorCoverage(), OWNER);

        assertThat(details.usesTobacco()).isTrue();
        assertThat(details.conditions()).containsExactlyInAnyOrder(HealthCondition.DIABETES, HealthCondition.HYPERTENSION);
    }

    @Test
    void getQuoteService_rejectsUnknownId() {
        assertThatThrownBy(() -> getQuoteService.getQuote(UUID.randomUUID(), AS_OWNER))
                .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void getQuoteService_rejectsAnotherUsersQuoteButAllowsAdministrators() {
        var id = createQuoteService.create(ADULT, OTHER_OWNER).id();

        assertThatThrownBy(() -> getQuoteService.getQuote(id, AS_OWNER)).isInstanceOf(QuoteNotFoundException.class);
        assertThat(getQuoteService.getQuote(id, AS_ADMIN).id()).isEqualTo(id);
    }

    @Test
    void updateCoverageService_rejectsAnotherUsersQuoteEvenForAnAdministrator() {
        var id = createQuoteService.create(ADULT, OTHER_OWNER).id();

        assertThatThrownBy(() -> updateCoverageService.updateCoverage(id, plainCoverage(), AS_ADMIN.id()))
                .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void markQuoteSubmittedService_transitionsAndPersists() {
        var id = createQuoteService.create(ADULT, OWNER).id();
        updateCoverageService.updateCoverage(id, plainCoverage(), OWNER);

        var details = markQuoteSubmittedService.markSubmitted(id, OWNER);

        assertThat(details.status()).isEqualTo(QuoteStatusView.SUBMITTED);
        assertThat(repository.findById(id, OWNER).orElseThrow().status()).isEqualTo(QuoteStatus.SUBMITTED);
    }

    @Test
    void searchQuotesService_returnsFilteredOrderedPageMetadata() {
        var jane = createQuoteService.create(ADULT, OWNER);
        updateCoverageService.updateCoverage(jane.id(), plainCoverage(), OWNER);
        markQuoteSubmittedService.markSubmitted(jane.id(), OWNER);
        createQuoteService.create(SENIOR, OWNER);

        var result = searchQuotesService.searchQuotes(
                SearchQuotesQuery.of(0, 1, "jane", "SUBMITTED", "STANDARD", "name", "asc"), AS_OWNER);

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
    void searchQuotesService_enforcesOwnerIsolationButAllowsAdministrators() {
        createQuoteService.create(ADULT, OWNER);
        createQuoteService.create(SENIOR, OTHER_OWNER);

        assertThat(searchQuotesService.searchQuotes(SearchQuotesQuery.defaults(), AS_OWNER).totalElements()).isEqualTo(1);
        assertThat(searchQuotesService.searchQuotes(SearchQuotesQuery.defaults(), AS_ADMIN).totalElements()).isEqualTo(2);
    }

    @Test
    void getQuoteSummaryService_returnsAggregateMetricsAndSevenDayTrend() {
        var draft = createQuoteService.create(ADULT, OWNER);
        var submitted = createQuoteService.create(SENIOR, OWNER);
        updateCoverageService.updateCoverage(submitted.id(), plainCoverage(), OWNER);
        markQuoteSubmittedService.markSubmitted(submitted.id(), OWNER);
        var failed = createQuoteService.create(
                new CreateQuoteCommand("Failed Quote", "failed@example.com", 40, "06600"), OWNER);
        markQuoteSubmissionFailedService.markSubmissionFailed(failed.id(), OWNER);

        var result = getQuoteSummaryService.getSummary(AS_OWNER);

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
        assertThat(draft.status()).isEqualTo(QuoteStatusView.DRAFT);
    }
}
