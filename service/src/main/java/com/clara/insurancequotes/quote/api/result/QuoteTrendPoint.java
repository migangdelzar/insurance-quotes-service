package com.clara.insurancequotes.quote.api.result;

import java.time.LocalDate;

/** One daily point within the public quote summary trend. */
public record QuoteTrendPoint(LocalDate date, long created, long submitted, long failed) {}
