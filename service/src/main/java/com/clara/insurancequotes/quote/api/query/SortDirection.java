package com.clara.insurancequotes.quote.api.query;

import java.util.Locale;

public enum SortDirection {
    ASC,
    DESC;

    public static SortDirection from(String value) {
        if (value == null || value.isBlank()) {
            return DESC;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "asc", "ascending" -> ASC;
            case "desc", "descending" -> DESC;
            default -> throw new IllegalArgumentException("Unsupported quote sort direction: " + value);
        };
    }
}
