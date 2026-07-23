package com.clara.insurancequotes.pricing.api.port.in;

import com.clara.insurancequotes.pricing.api.model.Premium;
import com.clara.insurancequotes.pricing.api.model.PricingInput;

public interface PremiumCalculator {

    Premium calculate(PricingInput input);
}
