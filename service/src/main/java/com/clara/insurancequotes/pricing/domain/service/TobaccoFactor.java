package com.clara.insurancequotes.pricing.domain.service;

import com.clara.insurancequotes.pricing.api.command.CalculatePremiumCommand;
import java.math.BigDecimal;

public class TobaccoFactor implements PremiumFactor {

    private static final BigDecimal TOBACCO = new BigDecimal("1.2");

    @Override
    public BigDecimal multiplier(CalculatePremiumCommand command) {
        return command.usesTobacco() ? TOBACCO : BigDecimal.ONE;
    }
}
