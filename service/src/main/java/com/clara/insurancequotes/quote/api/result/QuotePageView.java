package com.clara.insurancequotes.quote.api.result;

import java.util.List;

public record QuotePageView(
        List<QuoteView> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) {}
