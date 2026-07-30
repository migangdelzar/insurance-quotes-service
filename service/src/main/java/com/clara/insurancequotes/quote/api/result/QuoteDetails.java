package com.clara.insurancequotes.quote.api.result;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.api.type.HealthCondition;
import com.clara.insurancequotes.quote.api.type.QuoteStatusView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Stable public details returned for one quote. */
public record QuoteDetails(
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
        QuoteStatusView status,
        Instant createdAt,
        Instant updatedAt) {

    public QuoteDetails {
        conditions = conditions == null ? null : Set.copyOf(conditions);
    }
}
