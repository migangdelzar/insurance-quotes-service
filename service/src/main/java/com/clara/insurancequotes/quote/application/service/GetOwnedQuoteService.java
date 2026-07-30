package com.clara.insurancequotes.quote.application.service;

import com.clara.insurancequotes.quote.api.exception.QuoteNotFoundException;
import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import com.clara.insurancequotes.quote.api.usecase.GetOwnedQuoteUseCase;
import com.clara.insurancequotes.quote.application.mapper.QuoteApplicationMapper;
import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetOwnedQuoteService implements GetOwnedQuoteUseCase {

    private final QuoteRepository repository;

    @Override
    @Transactional(readOnly = true)
    public QuoteDetails getOwnedQuote(UUID id, UUID ownerId) {
        return QuoteApplicationMapper.toDetails(
                repository.findById(id, ownerId).orElseThrow(() -> new QuoteNotFoundException(id)));
    }
}
