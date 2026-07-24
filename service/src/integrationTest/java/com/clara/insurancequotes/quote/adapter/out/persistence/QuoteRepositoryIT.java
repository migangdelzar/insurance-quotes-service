package com.clara.insurancequotes.quote.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.quote.domain.model.QuoteMother;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import com.clara.insurancequotes.testsupport.Containers;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaQuoteRepository.class)
class QuoteRepositoryIT {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        Containers.registerPostgres(registry);
        Containers.registerRedis(registry);
    }

    @Autowired
    private JpaQuoteRepository repository;

    @Test
    void savesAndReloadsAggregateWithHealthProfile() {
        var saved = repository.save(QuoteMother.submittableSeniorDraft());

        var reloaded = repository.findById(saved.id()).orElseThrow();

        assertThat(reloaded.healthProfile().conditions()).isNotEmpty();
        assertThat(reloaded.monthlyPremium()).isEqualByComparingTo("327.60");
    }

    @Test
    void markExpired_batchUpdatesOnlyGivenIds() {
        var stale = repository.save(QuoteMother.draft());
        var fresh = repository.save(QuoteMother.submittableDraft());

        var cutoff = QuoteMother.FIXED_NOW.plus(Duration.ofMinutes(31));
        var ids = repository.findIdsToExpire(cutoff);
        var updated = repository.markExpired(ids, cutoff);

        assertThat(updated).isEqualTo(2);
        assertThat(repository.findById(stale.id()).orElseThrow().status()).isEqualTo(QuoteStatus.EXPIRED);
        assertThat(repository.findById(fresh.id()).orElseThrow().status()).isEqualTo(QuoteStatus.EXPIRED);
    }
}
