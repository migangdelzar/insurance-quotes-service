package com.clara.insurancequotes.quote.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.config.BusinessMetrics;
import com.clara.insurancequotes.quote.domain.event.QuoteExpired;
import com.clara.insurancequotes.quote.domain.model.QuoteMother;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import com.clara.insurancequotes.testsupport.InMemoryQuoteRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class DraftExpirationJobTest {

    private final InMemoryQuoteRepository repository = new InMemoryQuoteRepository();
    private final List<Object> publishedEvents = new ArrayList<>();
    private final ApplicationEventPublisher events = publishedEvents::add;

    @Test
    void expiresOnlyDraftsOlderThanTtl_andPublishesOneEventPerQuote() {
        var stale = repository.save(QuoteMother.draft());
        var submitted = repository.save(QuoteMother.submittableDraft());
        submitted.markSubmitted(QuoteMother.FIXED_NOW);

        var clockAfterTtl = Clock.fixed(QuoteMother.FIXED_NOW.plus(Duration.ofMinutes(31)), ZoneOffset.UTC);
        var job = new DraftExpirationJob(
                repository,
                events,
                clockAfterTtl,
                Duration.ofMinutes(30),
                new BusinessMetrics(new SimpleMeterRegistry()));

        var expired = job.expireStaleDrafts();

        assertThat(expired).isEqualTo(1);
        assertThat(repository.findById(stale.id(), null).orElseThrow().status()).isEqualTo(QuoteStatus.EXPIRED);
        assertThat(repository.findById(submitted.id(), null).orElseThrow().status())
                .isEqualTo(QuoteStatus.SUBMITTED);
        assertThat(publishedEvents).containsExactly(new QuoteExpired(stale.id(), stale.userId()));
    }

    @Test
    void noStaleDrafts_returnsZeroAndPublishesNothing() {
        repository.save(QuoteMother.draft());
        var clockWithinTtl = Clock.fixed(QuoteMother.FIXED_NOW.plus(Duration.ofMinutes(5)), ZoneOffset.UTC);
        var job = new DraftExpirationJob(
                repository,
                events,
                clockWithinTtl,
                Duration.ofMinutes(30),
                new BusinessMetrics(new SimpleMeterRegistry()));

        assertThat(job.expireStaleDrafts()).isZero();
        assertThat(publishedEvents).isEmpty();
    }
}
