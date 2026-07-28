package com.clara.insurancequotes.quote.application.port.out;

import java.util.UUID;

public record StaleQuoteRef(UUID id, UUID ownerId) {}
