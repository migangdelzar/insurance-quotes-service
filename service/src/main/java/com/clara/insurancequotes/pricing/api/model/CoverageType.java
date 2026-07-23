package com.clara.insurancequotes.pricing.api.model;

import java.math.BigDecimal;

public enum CoverageType {
    BASIC(new BigDecimal("50")),
    STANDARD(new BigDecimal("100")),
    PREMIUM(new BigDecimal("200"));

    private final BigDecimal basePremium;

    CoverageType(BigDecimal basePremium) {
        this.basePremium = basePremium;
    }

    public BigDecimal basePremium() {
        return basePremium;
    }
}
