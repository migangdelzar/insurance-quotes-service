package com.clara.insurancequotes.pricing.domain.policy;

import com.clara.insurancequotes.pricing.api.model.PricingInput;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ConditionsFactor implements PremiumFactor {

    private static final BigDecimal ANY_CONDITION = new BigDecimal("1.3");

    @Override
    public BigDecimal multiplier(PricingInput input) {
        return input.hasPreexistingConditions() ? ANY_CONDITION : BigDecimal.ONE;
    }
}
