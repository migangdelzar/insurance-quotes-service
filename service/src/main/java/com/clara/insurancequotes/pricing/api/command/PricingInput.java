package com.clara.insurancequotes.pricing.api.command;

import com.clara.insurancequotes.pricing.api.type.CoverageType;

public record PricingInput(
        CoverageType coverageType,
        int age,
        boolean hasPreexistingConditions,
        boolean usesTobacco,
        boolean needsSpouseCoverage) {}
