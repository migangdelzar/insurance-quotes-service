package com.clara.insurancequotes.pricing.api.usecase;

import com.clara.insurancequotes.pricing.api.command.CalculatePremiumCommand;
import com.clara.insurancequotes.pricing.api.result.Premium;

public interface CalculatePremiumUseCase {

    Premium calculate(CalculatePremiumCommand command);
}
