package com.clara.insurancequotes.quote.application.port.out;

import com.clara.insurancequotes.quote.domain.model.Quote;
import java.util.List;

public record QuoteSearchResult(List<Quote> content, int page, int size, long totalElements) {

    public QuoteSearchResult {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must not be negative");
        }
        content = List.copyOf(content);
    }

    public int totalPages() {
        return (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }

    public boolean hasPrevious() {
        return page > 0;
    }
}
