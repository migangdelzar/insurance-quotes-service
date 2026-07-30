package com.clara.insurancequotes.quote.application.service;

import com.clara.insurancequotes.quote.api.query.SearchQuotesQuery;
import com.clara.insurancequotes.quote.api.result.QuotePage;
import com.clara.insurancequotes.quote.api.type.RequestingUser;
import com.clara.insurancequotes.quote.api.usecase.SearchQuotesUseCase;
import com.clara.insurancequotes.quote.application.mapper.QuoteApplicationMapper;
import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchQuotesService implements SearchQuotesUseCase {

    private final QuoteRepository repository;

    @Override
    @Transactional(readOnly = true)
    public QuotePage searchQuotes(SearchQuotesQuery query, RequestingUser requester) {
        var result = repository.findPage(query, requester.admin() ? null : requester.id());
        return new QuotePage(
                result.content().stream().map(QuoteApplicationMapper::toDetails).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.hasNext(),
                result.hasPrevious());
    }
}
