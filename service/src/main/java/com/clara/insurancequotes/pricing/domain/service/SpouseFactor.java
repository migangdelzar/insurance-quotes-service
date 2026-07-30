package com.clara.insurancequotes.pricing.domain.service;

import com.clara.insurancequotes.pricing.api.command.CalculatePremiumCommand;
import java.math.BigDecimal;

public class SpouseFactor implements PremiumFactor {

    private static final BigDecimal SPOUSE = new BigDecimal("1.4");

    @Override
    public BigDecimal multiplier(CalculatePremiumCommand command) {
        return command.needsSpouseCoverage() ? SPOUSE : BigDecimal.ONE;
    }
}
