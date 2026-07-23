package com.clara.insurancequotes.pricing.application.service;

import com.clara.insurancequotes.pricing.api.Premium;
import com.clara.insurancequotes.pricing.api.PremiumCalculator;
import com.clara.insurancequotes.pricing.api.PricingInput;
import com.clara.insurancequotes.pricing.domain.policy.PremiumFactor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultPremiumCalculator implements PremiumCalculator {

    private final List<PremiumFactor> factors;

    public DefaultPremiumCalculator(List<PremiumFactor> factors) {
        this.factors = List.copyOf(factors);
    }

    @Override
    public Premium calculate(PricingInput input) {
        var combinedMultiplier =
                factors.stream().map(factor -> factor.multiplier(input)).reduce(BigDecimal.ONE, BigDecimal::multiply);
        var monthly =
                input.coverageType().basePremium().multiply(combinedMultiplier).setScale(2, RoundingMode.HALF_UP);
        return new Premium(monthly);
    }
}
