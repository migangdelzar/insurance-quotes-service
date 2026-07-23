package com.clara.insurancequotes.quote.adapter.in.web.request;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.type.HealthCondition;
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
