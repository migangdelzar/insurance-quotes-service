package com.clara.insurancequotes.pricing.domain.policy;

import com.clara.insurancequotes.pricing.api.PricingInput;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class AgeFactor implements PremiumFactor {

    private static final BigDecimal OVER_65 = new BigDecimal("1.5");
    private static final int AGE_THRESHOLD = 65;

    @Override
    public BigDecimal multiplier(PricingInput input) {
        return input.age() > AGE_THRESHOLD ? OVER_65 : BigDecimal.ONE;
    }
}
