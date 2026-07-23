package com.clara.insurancequotes.quote.infrastructure.inbound.web.model;

import com.clara.insurancequotes.pricing.api.model.CoverageType;
import com.clara.insurancequotes.quote.api.model.HealthCondition;
import com.clara.insurancequotes.quote.api.model.UpdateCoverageCommand;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record UpdateCoverageRequest(
        @NotNull CoverageType coverageType,
        Boolean hasPreexistingConditions,
        Set<HealthCondition> conditions,
        Boolean takesPrescriptionMedication,
        Boolean usesTobacco,
        Boolean needsSpouseCoverage) {

    public UpdateCoverageCommand toCommand() {
        return new UpdateCoverageCommand(
                coverageType,
                hasPreexistingConditions,
                conditions,
                takesPrescriptionMedication,
                usesTobacco,
                needsSpouseCoverage);
    }
}
