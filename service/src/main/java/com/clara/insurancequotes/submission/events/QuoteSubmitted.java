package com.clara.insurancequotes.submission.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("quote-submitted::#{#this.quoteId().toString()}")
public record QuoteSubmitted(UUID quoteId, BigDecimal monthlyPremium, Instant submittedAt) {}
