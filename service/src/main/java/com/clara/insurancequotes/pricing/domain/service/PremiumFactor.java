package com.clara.insurancequotes.pricing.domain.service;

import com.clara.insurancequotes.pricing.api.command.CalculatePremiumCommand;
import java.math.BigDecimal;

public interface PremiumFactor {

    BigDecimal multiplier(CalculatePremiumCommand command);
}
