package com.clara.insurancequotes.pricing.application.service;

import com.clara.insurancequotes.pricing.api.command.CalculatePremiumCommand;
import com.clara.insurancequotes.pricing.api.result.Premium;
import com.clara.insurancequotes.pricing.api.usecase.CalculatePremiumUseCase;
import com.clara.insurancequotes.pricing.domain.service.PremiumFactor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CalculatePremiumService implements CalculatePremiumUseCase {

    private final List<PremiumFactor> factors;

    public CalculatePremiumService(List<PremiumFactor> factors) {
        this.factors = List.copyOf(factors);
    }

    @Override
    public Premium calculate(CalculatePremiumCommand command) {
        var combinedMultiplier =
                factors.stream().map(factor -> factor.multiplier(command)).reduce(BigDecimal.ONE, BigDecimal::multiply);
        var monthly = command.coverageType()
                .basePremium()
                .multiply(combinedMultiplier)
                .setScale(2, RoundingMode.HALF_UP);
        return new Premium(monthly);
    }
}
