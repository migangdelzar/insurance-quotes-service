package com.clara.insurancequotes.quote.application.service;

import com.clara.insurancequotes.quote.api.exception.QuoteNotFoundException;
import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import com.clara.insurancequotes.quote.api.usecase.MarkQuoteSubmittedUseCase;
import com.clara.insurancequotes.quote.application.mapper.QuoteApplicationMapper;
import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import com.clara.insurancequotes.quote.configuration.QuoteCacheConfiguration;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarkQuoteSubmittedService implements MarkQuoteSubmittedUseCase {

    private final QuoteRepository repository;
    private final Clock clock;

    @Override
    @Transactional
    @CacheEvict(cacheNames = QuoteCacheConfiguration.QUOTES_CACHE, key = "#id + '|' + #ownerId", beforeInvocation = true)
    public QuoteDetails markSubmitted(UUID id, UUID ownerId) {
        var quote = repository.findById(id, ownerId).orElseThrow(() -> new QuoteNotFoundException(id));
        quote.markSubmitted(clock.instant());
        return QuoteApplicationMapper.toDetails(repository.save(quote));
    }
}
