package com.clara.insurancequotes.pricing.domain.policy;

import com.clara.insurancequotes.pricing.api.PricingInput;
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
