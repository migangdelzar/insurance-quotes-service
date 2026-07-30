package com.clara.insurancequotes.quote.application.service;

import com.clara.insurancequotes.pricing.api.command.CalculatePremiumCommand;
import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.pricing.api.usecase.CalculatePremiumUseCase;
import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.api.result.QuoteDistributionView;
import com.clara.insurancequotes.quote.api.result.QuotePageView;
import com.clara.insurancequotes.quote.api.result.QuoteSummaryView;
import com.clara.insurancequotes.quote.api.result.QuoteTrendPointView;
import com.clara.insurancequotes.quote.api.result.QuoteView;
import com.clara.insurancequotes.quote.api.usecase.QuoteApi;
import com.clara.insurancequotes.quote.api.usecase.RequestingUser;
import com.clara.insurancequotes.quote.application.exception.QuoteNotFoundException;
import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import com.clara.insurancequotes.quote.configuration.CacheConfig;
import com.clara.insurancequotes.quote.domain.exception.HealthDataNotAllowedException;
import com.clara.insurancequotes.quote.domain.model.HealthProfile;
import com.clara.insurancequotes.quote.domain.model.Quote;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import com.clara.insurancequotes.shared.observability.BusinessMetrics;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteService implements QuoteApi {

    private static final int HEALTH_DATA_AGE_THRESHOLD = 65;

    private final QuoteRepository repository;
    private final CalculatePremiumUseCase premiumCalculator;
    private final Clock clock;
    private final BusinessMetrics metrics;

    @Override
    @Transactional
    public QuoteView create(CreateQuoteCommand command, UUID ownerId) {
        var quote = Quote.createDraft(
                ownerId, command.name(), command.email(), command.age(), command.zipCode(), clock.instant());
        var saved = repository.save(quote);
        metrics.quoteCreated();
        log.debug("Created quote {}", saved.id());
        return QuoteView.from(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.QUOTES_CACHE, key = "#id + '|' + #ownerId", beforeInvocation = true)
    public QuoteView updateCoverage(UUID id, UpdateCoverageCommand command, UUID ownerId) {
        try {
            var quote = load(id, ownerId);
            rejectHealthDataForNonSeniors(quote, command);
            var premium =
                    metrics.timePremiumCalculation(() -> premiumCalculator.calculate(pricingInputOf(quote, command)));
            quote.updateCoverage(command.coverageType(), healthProfileOf(command), premium.monthly(), clock.instant());
            var view = QuoteView.from(repository.save(quote));
            metrics.coverageUpdated("success", command.coverageType().name());
            return view;
        } catch (HealthDataNotAllowedException exception) {
            metrics.coverageUpdated("rejected", command.coverageType().name());
            throw exception;
        } catch (RuntimeException exception) {
            metrics.coverageUpdated("failed", command.coverageType().name());
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.QUOTES_CACHE, key = "#id + '|' + #requester.id()")
    public QuoteView getQuote(UUID id, RequestingUser requester) {
        return QuoteView.from(load(id, requester.admin() ? null : requester.id()));
    }

    @Override
    @Transactional(readOnly = true)
    public QuotePageView listQuotes(QuoteQuery query, RequestingUser requester) {
        var result = repository.findPage(query, requester.admin() ? null : requester.id());
        return new QuotePageView(
                result.content().stream().map(QuoteView::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.hasNext(),
                result.hasPrevious());
    }

    @Override
    @Transactional(readOnly = true)
    public QuoteSummaryView getSummary(RequestingUser requester) {
        var data = repository.findSummary(clock.instant(), requester.admin() ? null : requester.id());
        var submitted = data.statusCounts().getOrDefault(QuoteStatus.SUBMITTED, 0L);
        var failed = data.statusCounts().getOrDefault(QuoteStatus.SUBMISSION_FAILED, 0L);
        var attempts = submitted + failed;
        var submissionRate = attempts == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(submitted)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(attempts), 2, RoundingMode.HALF_UP);
        var statusDistribution = Arrays.stream(QuoteStatus.values())
                .map(status -> new QuoteDistributionView(
                        status.name(), data.statusCounts().getOrDefault(status, 0L)))
                .toList();
        var coverageDistribution = Arrays.stream(CoverageType.values())
                .map(coverage -> new QuoteDistributionView(
                        coverage.name(), data.coverageCounts().getOrDefault(coverage, 0L)))
                .toList();
        var trend = data.trend().stream()
                .map(point -> new QuoteTrendPointView(point.date(), point.created(), point.submitted(), point.failed()))
                .toList();
        return new QuoteSummaryView(
                data.totalQuotes(),
                data.statusCounts().getOrDefault(QuoteStatus.DRAFT, 0L),
                submitted,
                failed,
                data.statusCounts().getOrDefault(QuoteStatus.EXPIRED, 0L),
                data.pricedQuotes(),
                data.totalMonthlyPremium(),
                data.averageMonthlyPremium(),
                submissionRate,
                statusDistribution,
                coverageDistribution,
                trend);
    }

    @Override
    @Transactional(readOnly = true)
    public QuoteView getOwnedQuote(UUID id, UUID ownerId) {
        return QuoteView.from(load(id, ownerId));
    }

    @Override
    @Transactional(readOnly = true)
    public QuoteView ensureSubmittable(UUID id, UUID ownerId) {
        var quote = load(id, ownerId);
        quote.ensureSubmittable();
        return QuoteView.from(quote);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.QUOTES_CACHE, key = "#id + '|' + #ownerId", beforeInvocation = true)
    public QuoteView markSubmitted(UUID id, UUID ownerId) {
        return transition(id, ownerId, quote -> quote.markSubmitted(clock.instant()));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.QUOTES_CACHE, key = "#id + '|' + #ownerId", beforeInvocation = true)
    public QuoteView markSubmissionFailed(UUID id, UUID ownerId) {
        return transition(id, ownerId, quote -> quote.markSubmissionFailed(clock.instant()));
    }

    private QuoteView transition(UUID id, UUID ownerId, Consumer<Quote> change) {
        var quote = load(id, ownerId);
        change.accept(quote);
        return QuoteView.from(repository.save(quote));
    }

    private Quote load(UUID id, UUID ownerId) {
        return repository.findById(id, ownerId).orElseThrow(() -> new QuoteNotFoundException(id));
    }

    private static void rejectHealthDataForNonSeniors(Quote quote, UpdateCoverageCommand command) {
        if (quote.age() <= HEALTH_DATA_AGE_THRESHOLD && command.carriesHealthData()) {
            throw new HealthDataNotAllowedException(quote.age());
        }
    }

    private static HealthProfile healthProfileOf(UpdateCoverageCommand command) {
        if (!command.carriesHealthData()) {
            return HealthProfile.none();
        }
        return new HealthProfile(
                command.hasPreexistingConditions(),
                command.conditions(),
                command.takesPrescriptionMedication(),
                command.usesTobacco(),
                command.needsSpouseCoverage());
    }

    private static CalculatePremiumCommand pricingInputOf(Quote quote, UpdateCoverageCommand command) {
        return new CalculatePremiumCommand(
                command.coverageType(),
                quote.age(),
                Boolean.TRUE.equals(command.hasPreexistingConditions()),
                Boolean.TRUE.equals(command.usesTobacco()),
                Boolean.TRUE.equals(command.needsSpouseCoverage()));
    }
}
