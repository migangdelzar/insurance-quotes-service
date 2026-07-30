package com.clara.insurancequotes.quote.application.service;

import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import com.clara.insurancequotes.quote.api.usecase.CreateQuoteUseCase;
import com.clara.insurancequotes.quote.application.mapper.QuoteApplicationMapper;
import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import com.clara.insurancequotes.quote.domain.model.Quote;
import com.clara.insurancequotes.shared.observability.BusinessMetrics;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateQuoteService implements CreateQuoteUseCase {

    private final QuoteRepository repository;
    private final Clock clock;
    private final BusinessMetrics metrics;

    @Override
    @Transactional
    public QuoteDetails create(CreateQuoteCommand command, UUID ownerId) {
        var quote = Quote.createDraft(
                ownerId, command.name(), command.email(), command.age(), command.zipCode(), clock.instant());
        var saved = repository.save(quote);
        metrics.quoteCreated();
        log.debug("Created quote {}", saved.id());
        return QuoteApplicationMapper.toDetails(saved);
    }
}
