package com.clara.insurancequotes.quote.application.service;

import com.clara.insurancequotes.quote.api.exception.QuoteNotFoundException;
import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import com.clara.insurancequotes.quote.api.type.RequestingUser;
import com.clara.insurancequotes.quote.api.usecase.GetQuoteUseCase;
import com.clara.insurancequotes.quote.application.mapper.QuoteApplicationMapper;
import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import com.clara.insurancequotes.quote.configuration.CacheConfig;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetQuoteService implements GetQuoteUseCase {

    private final QuoteRepository repository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheConfig.QUOTES_CACHE,
            key = "#id + '|' + #requester.id()",
            condition = "!#requester.admin()")
    public QuoteDetails getQuote(UUID id, RequestingUser requester) {
        return QuoteApplicationMapper.toDetails(repository
                .findById(id, requester.admin() ? null : requester.id())
                .orElseThrow(() -> new QuoteNotFoundException(id)));
    }
}
