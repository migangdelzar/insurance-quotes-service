package com.clara.insurancequotes.pricing.domain.policy;

import com.clara.insurancequotes.pricing.api.PricingInput;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class SpouseFactor implements PremiumFactor {

    private static final BigDecimal SPOUSE = new BigDecimal("1.4");

    @Override
    public BigDecimal multiplier(PricingInput input) {
        return input.needsSpouseCoverage() ? SPOUSE : BigDecimal.ONE;
    }
}
