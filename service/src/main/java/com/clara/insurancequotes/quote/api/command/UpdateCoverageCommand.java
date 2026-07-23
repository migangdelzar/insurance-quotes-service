package com.clara.insurancequotes.quote.api.command;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.api.type.HealthCondition;
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
