package com.clara.insurancequotes.pricing.api.usecase;

import com.clara.insurancequotes.pricing.api.command.PricingInput;
import com.clara.insurancequotes.pricing.api.result.Premium;

public interface PremiumCalculator {

    Premium calculate(PricingInput input);
}
