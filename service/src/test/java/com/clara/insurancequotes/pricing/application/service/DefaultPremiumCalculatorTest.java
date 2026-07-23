package com.clara.insurancequotes.pricing.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.pricing.api.command.PricingInput;
import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.pricing.domain.service.AgeFactor;
import com.clara.insurancequotes.pricing.domain.service.ConditionsFactor;
import com.clara.insurancequotes.pricing.domain.service.SpouseFactor;
import com.clara.insurancequotes.pricing.domain.service.TobaccoFactor;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultPremiumCalculatorTest {

    private final DefaultPremiumCalculator calculator = new DefaultPremiumCalculator(
            List.of(new AgeFactor(), new ConditionsFactor(), new TobaccoFactor(), new SpouseFactor()));

    @Test
    void specWorkedExample_age70StandardOneConditionSmokerWithSpouse_is327_60() {
        var input = new PricingInput(CoverageType.STANDARD, 70, true, true, true);

        var premium = calculator.calculate(input);

        assertThat(premium.monthly()).isEqualByComparingTo(new BigDecimal("327.60"));
    }

    @Test
    void noFactorsApply_basicAt30_isBasePremium() {
        var input = new PricingInput(CoverageType.BASIC, 30, false, false, false);

        assertThat(calculator.calculate(input).monthly()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void ageBoundary_exactly65_doesNotApplyAgeMultiplier() {
        var input = new PricingInput(CoverageType.PREMIUM, 65, false, false, false);

        assertThat(calculator.calculate(input).monthly()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void ageBoundary_66_appliesAgeMultiplier() {
        var input = new PricingInput(CoverageType.PREMIUM, 66, false, false, false);

        assertThat(calculator.calculate(input).monthly()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void resultAlwaysHasTwoDecimals_halfUpRounding() {
        var input = new PricingInput(CoverageType.BASIC, 70, true, true, false);

        assertThat(calculator.calculate(input).monthly()).isEqualTo(new BigDecimal("117.00"));
    }
}
