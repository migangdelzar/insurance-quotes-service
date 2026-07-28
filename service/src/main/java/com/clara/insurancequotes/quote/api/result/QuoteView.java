package com.clara.insurancequotes.quote.api.result;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.api.type.HealthCondition;
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
                copyConditions(health.conditions()),
                health.takesPrescriptionMedication(),
                health.usesTobacco(),
                health.needsSpouseCoverage(),
                quote.monthlyPremium(),
                quote.status(),
                quote.createdAt(),
                quote.updatedAt());
    }

    private static Set<HealthCondition> copyConditions(Set<HealthCondition> conditions) {
        return conditions == null ? null : Set.copyOf(conditions);
    }
}
