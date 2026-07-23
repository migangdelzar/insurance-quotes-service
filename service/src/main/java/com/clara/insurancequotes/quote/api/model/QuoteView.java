package com.clara.insurancequotes.quote.api.model;

import com.clara.insurancequotes.pricing.api.model.CoverageType;
import com.clara.insurancequotes.quote.domain.model.Quote;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record QuoteView(
        UUID id,
        String name,
        String email,
        int age,
        String zipCode,
        CoverageType coverageType,
        Boolean hasPreexistingConditions,
        Set<HealthCondition> conditions,
        Boolean takesPrescriptionMedication,
        Boolean usesTobacco,
        Boolean needsSpouseCoverage,
        BigDecimal monthlyPremium,
        QuoteStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static QuoteView from(Quote quote) {
        var health = quote.healthProfile();
        return new QuoteView(
                quote.id(),
                quote.name(),
                quote.email(),
                quote.age(),
                quote.zipCode(),
                quote.coverageType(),
                health.hasPreexistingConditions(),
                health.conditions(),
                health.takesPrescriptionMedication(),
                health.usesTobacco(),
                health.needsSpouseCoverage(),
                quote.monthlyPremium(),
                quote.status(),
                quote.createdAt(),
                quote.updatedAt());
    }
}
