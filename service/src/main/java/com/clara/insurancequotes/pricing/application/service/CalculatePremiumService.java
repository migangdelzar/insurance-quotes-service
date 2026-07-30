package com.clara.insurancequotes.pricing.application.service;

import com.clara.insurancequotes.pricing.api.command.CalculatePremiumCommand;
import com.clara.insurancequotes.pricing.api.result.Premium;
import com.clara.insurancequotes.pricing.api.usecase.CalculatePremiumUseCase;
import com.clara.insurancequotes.pricing.domain.service.PremiumCalculator;
import org.springframework.stereotype.Service;

@Service
public class CalculatePremiumService implements CalculatePremiumUseCase {

    private final PremiumCalculator calculator;

    public CalculatePremiumService(PremiumCalculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public Premium calculate(CalculatePremiumCommand command) {
        return new Premium(calculator.calculate(command));
    }
}
