package com.clara.insurancequotes.quote.application.service;

import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
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

    public DraftExpirationJob(
            QuoteRepository repository,
            ApplicationEventPublisher events,
            Clock clock,
            @Value("${quote.expiration.draft-ttl}") Duration draftTtl) {
        this.repository = repository;
        this.events = events;
        this.clock = clock;
        this.draftTtl = draftTtl;
    }

    @Transactional
    public int expireStaleDrafts() {
        var now = clock.instant();
        var staleIds = repository.findIdsToExpire(now.minus(draftTtl));
        if (staleIds.isEmpty()) {
            return 0;
        }
        var expired = repository.markExpired(staleIds, now);
        staleIds.forEach(id -> events.publishEvent(new QuoteExpired(id)));
        log.info("Expired {} stale draft quotes", expired);
        return expired;
    }
}
