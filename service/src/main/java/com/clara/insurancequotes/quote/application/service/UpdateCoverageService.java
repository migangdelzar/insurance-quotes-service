package com.clara.insurancequotes.quote.application.service;

import com.clara.insurancequotes.pricing.api.command.CalculatePremiumCommand;
import com.clara.insurancequotes.pricing.api.usecase.CalculatePremiumUseCase;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.exception.QuoteNotFoundException;
import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import com.clara.insurancequotes.quote.api.usecase.UpdateCoverageUseCase;
import com.clara.insurancequotes.quote.application.mapper.QuoteApplicationMapper;
import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import com.clara.insurancequotes.quote.configuration.CacheConfig;
import com.clara.insurancequotes.quote.domain.exception.HealthDataNotAllowedException;
import com.clara.insurancequotes.quote.domain.model.HealthProfile;
import com.clara.insurancequotes.quote.domain.model.Quote;
import com.clara.insurancequotes.shared.observability.BusinessMetrics;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCoverageService implements UpdateCoverageUseCase {

    private static final int HEALTH_DATA_AGE_THRESHOLD = 65;

    private final QuoteRepository repository;
    private final CalculatePremiumUseCase premiumCalculator;
    private final Clock clock;
    private final BusinessMetrics metrics;

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.QUOTES_CACHE, key = "#id + '|' + #ownerId", beforeInvocation = true)
    public QuoteDetails updateCoverage(UUID id, UpdateCoverageCommand command, UUID ownerId) {
        try {
            var quote = load(id, ownerId);
            rejectHealthDataForNonSeniors(quote, command);
            var premium = metrics.timePremiumCalculation(
                    () -> premiumCalculator.calculate(calculatePremiumCommandOf(quote, command)));
            quote.updateCoverage(command.coverageType(), healthProfileOf(command), premium.monthly(), clock.instant());
            var details = QuoteApplicationMapper.toDetails(repository.save(quote));
            metrics.coverageUpdated("success", command.coverageType().name());
            return details;
        } catch (HealthDataNotAllowedException exception) {
            metrics.coverageUpdated("rejected", command.coverageType().name());
            throw exception;
        } catch (RuntimeException exception) {
            metrics.coverageUpdated("failed", command.coverageType().name());
            throw exception;
        }
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

    private static CalculatePremiumCommand calculatePremiumCommandOf(Quote quote, UpdateCoverageCommand command) {
        return new CalculatePremiumCommand(
                command.coverageType(),
                quote.age(),
                Boolean.TRUE.equals(command.hasPreexistingConditions()),
                Boolean.TRUE.equals(command.usesTobacco()),
                Boolean.TRUE.equals(command.needsSpouseCoverage()));
    }
}
