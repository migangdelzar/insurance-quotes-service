package com.clara.insurancequotes.quote.api.model;

import com.clara.insurancequotes.pricing.api.model.CoverageType;
import java.util.Set;

public record UpdateCoverageCommand(
        CoverageType coverageType,
        Boolean hasPreexistingConditions,
        Set<HealthCondition> conditions,
        Boolean takesPrescriptionMedication,
        Boolean usesTobacco,
        Boolean needsSpouseCoverage) {

    public boolean carriesHealthData() {
        return hasPreexistingConditions != null
                || (conditions != null && !conditions.isEmpty())
                || takesPrescriptionMedication != null
                || usesTobacco != null
                || needsSpouseCoverage != null;
    }
}
