package com.clara.insurancequotes.quote.api.query;

import java.util.Locale;

public enum QuoteSortField {
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt"),
    NAME("name"),
    MONTHLY_PREMIUM("monthlyPremium"),
    STATUS("status");

    private final String property;

    QuoteSortField(String property) {
        this.property = property;
    }

    public String property() {
        return property;
    }

    public static QuoteSortField from(String value) {
        if (value == null || value.isBlank()) {
            return CREATED_AT;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "createdat", "created_at" -> CREATED_AT;
            case "updatedat", "updated_at" -> UPDATED_AT;
            case "name" -> NAME;
            case "monthlypremium", "monthly_premium", "premium" -> MONTHLY_PREMIUM;
            case "status" -> STATUS;
            default -> throw new IllegalArgumentException("Unsupported quote sort field: " + value);
        };
    }
}
