package com.clara.insurancequotes.quote.api.result;

import java.time.LocalDate;

public record QuoteTrendPointView(LocalDate date, long created, long submitted, long failed) {}
