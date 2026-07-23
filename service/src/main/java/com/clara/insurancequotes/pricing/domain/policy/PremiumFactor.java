package com.clara.insurancequotes.pricing.domain.policy;

import com.clara.insurancequotes.pricing.api.model.PricingInput;
import java.math.BigDecimal;

public interface PremiumFactor {

    BigDecimal multiplier(PricingInput input);
}
