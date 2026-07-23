package com.clara.insurancequotes.pricing.api.model;

public record PricingInput(
        CoverageType coverageType,
        int age,
        boolean hasPreexistingConditions,
        boolean usesTobacco,
        boolean needsSpouseCoverage) {}
