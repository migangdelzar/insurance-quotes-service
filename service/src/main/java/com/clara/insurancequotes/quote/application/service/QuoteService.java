package com.clara.insurancequotes.quote.application.service;

import com.clara.insurancequotes.config.BusinessMetrics;
import com.clara.insurancequotes.pricing.api.command.PricingInput;
import com.clara.insurancequotes.pricing.api.usecase.PremiumCalculator;
import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.api.result.QuotePageView;
import com.clara.insurancequotes.quote.api.result.QuoteView;
import com.clara.insurancequotes.quote.api.usecase.QuoteApi;
import com.clara.insurancequotes.quote.application.exception.QuoteNotFoundException;
import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import com.clara.insurancequotes.quote.configuration.CacheConfig;
import com.clara.insurancequotes.quote.domain.exception.HealthDataNotAllowedException;
import com.clara.insurancequotes.quote.domain.model.HealthProfile;
import com.clara.insurancequotes.quote.domain.model.Quote;
import java.time.Clock;
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
    private final PremiumCalculator premiumCalculator;
    private final Clock clock;
    private final BusinessMetrics metrics;

    @Override
    @Transactional
    public QuoteView create(CreateQuoteCommand command) {
        var quote =
                Quote.createDraft(command.name(), command.email(), command.age(), command.zipCode(), clock.instant());
        var saved = repository.save(quote);
        metrics.quoteCreated();
        log.debug("Created quote {}", saved.id());
        return QuoteView.from(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.QUOTES_CACHE, key = "#id", beforeInvocation = true)
    public QuoteView updateCoverage(UUID id, UpdateCoverageCommand command) {
        try {
            var quote = load(id);
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
    @Cacheable(cacheNames = CacheConfig.QUOTES_CACHE, key = "#id")
    public QuoteView getQuote(UUID id) {
        return QuoteView.from(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public QuotePageView listQuotes(QuoteQuery query) {
        var result = repository.findPage(query);
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
    public QuoteView ensureSubmittable(UUID id) {
        var quote = load(id);
        quote.ensureSubmittable();
        return QuoteView.from(quote);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.QUOTES_CACHE, key = "#id", beforeInvocation = true)
    public QuoteView markSubmitted(UUID id) {
        return transition(id, quote -> quote.markSubmitted(clock.instant()));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.QUOTES_CACHE, key = "#id", beforeInvocation = true)
    public QuoteView markSubmissionFailed(UUID id) {
        return transition(id, quote -> quote.markSubmissionFailed(clock.instant()));
    }

    private QuoteView transition(UUID id, Consumer<Quote> change) {
        var quote = load(id);
        change.accept(quote);
        return QuoteView.from(repository.save(quote));
    }

    private Quote load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new QuoteNotFoundException(id));
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

    private static PricingInput pricingInputOf(Quote quote, UpdateCoverageCommand command) {
        return new PricingInput(
                command.coverageType(),
                quote.age(),
                Boolean.TRUE.equals(command.hasPreexistingConditions()),
                Boolean.TRUE.equals(command.usesTobacco()),
                Boolean.TRUE.equals(command.needsSpouseCoverage()));
    }
}
