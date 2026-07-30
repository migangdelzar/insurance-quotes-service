package com.clara.insurancequotes.quote.api.result;

import java.util.List;

/** Stable public page of quote search results. */
public record QuotePage(
        List<QuoteDetails> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) {}
