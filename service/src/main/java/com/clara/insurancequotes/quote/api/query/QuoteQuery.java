package com.clara.insurancequotes.quote.api.query;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import java.util.Locale;

public record QuoteQuery(
        int page,
        int size,
        String search,
        QuoteStatus status,
        CoverageType coverage,
        QuoteSortField sortBy,
        SortDirection direction) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public QuoteQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or greater");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
        search = normalizeSearch(search);
        sortBy = sortBy == null ? QuoteSortField.CREATED_AT : sortBy;
        direction = direction == null ? SortDirection.DESC : direction;
    }

    public static QuoteQuery of(
            int page, int size, String search, String status, String coverage, String sortBy, String direction) {
        return new QuoteQuery(
                page,
                size,
                search,
                parseEnum(status, QuoteStatus.class, "status"),
                parseEnum(coverage, CoverageType.class, "coverage"),
                QuoteSortField.from(sortBy),
                SortDirection.from(direction));
    }

    public static QuoteQuery defaults() {
        return new QuoteQuery(0, DEFAULT_SIZE, null, null, null, QuoteSortField.CREATED_AT, SortDirection.DESC);
    }

    private static String normalizeSearch(String value) {
        var normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static <T extends Enum<T>> T parseEnum(String value, Class<T> type, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported quote " + name + ": " + value, exception);
        }
    }
}
