package com.clara.insurancequotes.quote.domain.exception;

import java.util.UUID;

public class InvalidStateTransitionException extends QuoteException {

    public InvalidStateTransitionException(UUID quoteId, String currentStatus, String attempted) {
        super("Quote %s in status %s does not allow %s".formatted(quoteId, currentStatus, attempted));
    }
}
