package com.clara.insurancequotes.quote.domain.event;

import java.util.UUID;

/** In-memory completed fact consumed by the local cache listener. */
public record QuoteExpired(UUID quoteId) {}
