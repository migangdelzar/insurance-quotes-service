package com.clara.insurancequotes.quote.application.service;

import com.clara.insurancequotes.quote.api.exception.QuoteNotFoundException;
import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import com.clara.insurancequotes.quote.api.usecase.EnsureQuoteSubmittableUseCase;
import com.clara.insurancequotes.quote.application.mapper.QuoteApplicationMapper;
import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnsureQuoteSubmittableService implements EnsureQuoteSubmittableUseCase {

    private final QuoteRepository repository;

    @Override
    @Transactional(readOnly = true)
    public QuoteDetails ensureSubmittable(UUID id, UUID ownerId) {
        var quote = repository.findById(id, ownerId).orElseThrow(() -> new QuoteNotFoundException(id));
        quote.ensureSubmittable();
        return QuoteApplicationMapper.toDetails(quote);
    }
}
