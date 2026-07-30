package com.clara.insurancequotes.pricing.domain.service;

import com.clara.insurancequotes.pricing.api.command.CalculatePremiumCommand;
import java.math.BigDecimal;

public class AgeFactor implements PremiumFactor {

    private static final BigDecimal OVER_65 = new BigDecimal("1.5");
    private static final int AGE_THRESHOLD = 65;

    @Override
    public BigDecimal multiplier(CalculatePremiumCommand command) {
        return command.age() > AGE_THRESHOLD ? OVER_65 : BigDecimal.ONE;
    }
}
