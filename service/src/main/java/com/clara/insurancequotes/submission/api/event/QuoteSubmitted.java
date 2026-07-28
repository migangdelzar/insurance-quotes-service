package com.clara.insurancequotes.submission.api.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("quote-submitted::#{#this.quoteId().toString()}")
public record QuoteSubmitted(UUID quoteId, BigDecimal monthlyPremium, Instant submittedAt) {}
