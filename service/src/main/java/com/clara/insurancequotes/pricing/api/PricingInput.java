package com.clara.insurancequotes.pricing.api;

public record PricingInput(
        CoverageType coverageType,
        int age,
        boolean hasPreexistingConditions,
        boolean usesTobacco,
        boolean needsSpouseCoverage) {}
