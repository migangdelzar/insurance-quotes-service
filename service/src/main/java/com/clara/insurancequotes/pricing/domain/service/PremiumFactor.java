package com.clara.insurancequotes.pricing.domain.service;

import com.clara.insurancequotes.pricing.api.command.PricingInput;
import java.math.BigDecimal;

public interface PremiumFactor {

    BigDecimal multiplier(PricingInput input);
}
