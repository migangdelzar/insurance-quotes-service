package com.clara.insurancequotes.pricing.domain.service;

import com.clara.insurancequotes.pricing.api.command.PricingInput;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class TobaccoFactor implements PremiumFactor {

    private static final BigDecimal TOBACCO = new BigDecimal("1.2");

    @Override
    public BigDecimal multiplier(PricingInput input) {
        return input.usesTobacco() ? TOBACCO : BigDecimal.ONE;
    }
}
