package com.clara.insurancequotes.quote.api.usecase;

import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import java.util.UUID;

/** Creates a draft quote for its owner. */
public interface CreateQuoteUseCase {

    QuoteDetails create(CreateQuoteCommand command, UUID ownerId);
}
