package com.clara.insurancequotes.quote.api;

import com.clara.insurancequotes.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class InvalidStateTransitionException extends ApiException {

    public InvalidStateTransitionException(UUID quoteId, String currentStatus, String attempted) {
        super(
                "QUOTE_INVALID_STATE_TRANSITION",
                HttpStatus.CONFLICT,
                "Quote %s in status %s does not allow %s".formatted(quoteId, currentStatus, attempted));
    }
}
