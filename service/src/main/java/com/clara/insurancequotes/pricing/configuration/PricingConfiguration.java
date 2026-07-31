package com.clara.insurancequotes.pricing.configuration;

import com.clara.insurancequotes.pricing.domain.service.AgeFactor;
import com.clara.insurancequotes.pricing.domain.service.ConditionsFactor;
import com.clara.insurancequotes.pricing.domain.service.PremiumCalculationPolicy;
import com.clara.insurancequotes.pricing.domain.service.PremiumFactor;
import com.clara.insurancequotes.pricing.domain.service.SpouseFactor;
import com.clara.insurancequotes.pricing.domain.service.TobaccoFactor;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root for the pure pricing domain policies. */
@Configuration
public class PricingConfiguration {

    @Bean
    AgeFactor ageFactor() {
        return new AgeFactor();
    }

    @Bean
    ConditionsFactor conditionsFactor() {
        return new ConditionsFactor();
    }

    @Bean
    SpouseFactor spouseFactor() {
        return new SpouseFactor();
    }

    @Bean
    TobaccoFactor tobaccoFactor() {
        return new TobaccoFactor();
    }

    @Bean
    PremiumCalculationPolicy premiumCalculator(List<PremiumFactor> factors) {
        return new PremiumCalculationPolicy(factors);
    }
}
