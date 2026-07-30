package com.clara.insurancequotes.pricing.domain.service;

import com.clara.insurancequotes.pricing.api.command.CalculatePremiumCommand;
import java.math.BigDecimal;

public class ConditionsFactor implements PremiumFactor {

    private static final BigDecimal ANY_CONDITION = new BigDecimal("1.3");

    @Override
    public BigDecimal multiplier(CalculatePremiumCommand command) {
        return command.hasPreexistingConditions() ? ANY_CONDITION : BigDecimal.ONE;
    }
}
