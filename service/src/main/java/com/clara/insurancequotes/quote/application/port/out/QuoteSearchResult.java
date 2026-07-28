package com.clara.insurancequotes.quote.application.port.out;

import com.clara.insurancequotes.quote.domain.model.Quote;
import java.util.List;

public record QuoteSearchResult(List<Quote> content, int page, int size, long totalElements) {

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
