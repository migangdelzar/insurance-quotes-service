package com.clara.insurancequotes.quote.application.service;

import com.clara.insurancequotes.shared.observability.BusinessMetrics;
import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import com.clara.insurancequotes.quote.application.port.out.StaleQuoteRef;
import com.clara.insurancequotes.quote.domain.event.QuoteExpired;
import java.time.Clock;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class DraftExpirationJob {

    private final QuoteRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final Duration draftTtl;
    private final BusinessMetrics metrics;

    public DraftExpirationJob(
            QuoteRepository repository,
            ApplicationEventPublisher events,
            Clock clock,
            @Value("${quote.expiration.draft-ttl}") Duration draftTtl,
            BusinessMetrics metrics) {
        this.repository = repository;
        this.events = events;
        this.clock = clock;
        this.draftTtl = draftTtl;
        this.metrics = metrics;
    }

    @Transactional
    public int expireStaleDrafts() {
        var now = clock.instant();
        var staleDrafts = repository.findStaleDrafts(now.minus(draftTtl));
        if (staleDrafts.isEmpty()) {
            return 0;
        }
        var staleIds = staleDrafts.stream().map(StaleQuoteRef::id).toList();
        var expired = repository.markExpired(staleIds, now);
        staleDrafts.forEach(ref -> events.publishEvent(new QuoteExpired(ref.id(), ref.ownerId())));
        metrics.quotesExpired(expired);
        log.info("Expired {} stale draft quotes", expired);
        return expired;
    }
}
