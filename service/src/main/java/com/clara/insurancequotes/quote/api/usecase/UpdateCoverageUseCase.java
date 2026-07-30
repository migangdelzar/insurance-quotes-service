package com.clara.insurancequotes.quote.api.usecase;

import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import java.util.UUID;

/** Updates an owner's quote coverage and calculates its premium. */
public interface UpdateCoverageUseCase {

    QuoteDetails updateCoverage(UUID id, UpdateCoverageCommand command, UUID ownerId);
}
