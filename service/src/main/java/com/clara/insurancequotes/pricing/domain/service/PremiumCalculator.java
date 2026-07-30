package com.clara.insurancequotes.pricing.domain.service;

import com.clara.insurancequotes.pricing.api.command.CalculatePremiumCommand;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Applies pricing factors and owns premium arithmetic and rounding rules. */
public final class PremiumCalculator {

    private final List<PremiumFactor> factors;

    public PremiumCalculator(List<PremiumFactor> factors) {
        this.factors = List.copyOf(factors);
    }

    public BigDecimal calculate(CalculatePremiumCommand command) {
        var combinedMultiplier =
                factors.stream().map(factor -> factor.multiplier(command)).reduce(BigDecimal.ONE, BigDecimal::multiply);
        return command.coverageType().basePremium().multiply(combinedMultiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
