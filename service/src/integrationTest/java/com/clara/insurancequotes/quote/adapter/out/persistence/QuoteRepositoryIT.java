package com.clara.insurancequotes.quote.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.application.port.out.StaleQuoteRef;
import com.clara.insurancequotes.quote.domain.model.QuoteMother;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import com.clara.insurancequotes.testsupport.Containers;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedOwner() {
        insertUser(QuoteMother.OWNER_ID);
    }

    private void insertUser(UUID id) {
        jdbcTemplate.update(
                "insert into users (id, username, password_hash, role, created_at) values (?, ?, ?, ?, ?)",
                id,
                "user-" + id,
                "hash",
                "USER",
                Instant.now());
    }

    @Test
    void savesAndReloadsAggregateWithHealthProfile() {
        var saved = repository.save(QuoteMother.submittableSeniorDraft());

        var reloaded = repository.findById(saved.id(), null).orElseThrow();

        assertThat(reloaded.healthProfile().conditions()).isNotEmpty();
        assertThat(reloaded.monthlyPremium()).isEqualByComparingTo("327.60");
        assertThat(reloaded.userId()).isEqualTo(QuoteMother.OWNER_ID);
    }

    @Test
    void markExpired_batchUpdatesOnlyGivenIds() {
        var stale = repository.save(QuoteMother.draft());
        var fresh = repository.save(QuoteMother.submittableDraft());

        var cutoff = QuoteMother.FIXED_NOW.plus(Duration.ofMinutes(31));
        var ids = repository.findStaleDrafts(cutoff).stream()
                .map(StaleQuoteRef::id)
                .toList();
        var updated = repository.markExpired(ids, cutoff);

        assertThat(updated).isEqualTo(2);
        assertThat(repository.findById(stale.id(), null).orElseThrow().status()).isEqualTo(QuoteStatus.EXPIRED);
        assertThat(repository.findById(fresh.id(), null).orElseThrow().status()).isEqualTo(QuoteStatus.EXPIRED);
    }

    @Test
    void findById_scopedToOtherOwner_returnsEmpty() {
        var saved = repository.save(QuoteMother.draft());

        var asOwner = repository.findById(saved.id(), QuoteMother.OWNER_ID);
        var asOtherUser = repository.findById(saved.id(), UUID.randomUUID());

        assertThat(asOwner).isPresent();
        assertThat(asOtherUser).isEmpty();
    }

    @Test
    void findPage_scopedToOwner_excludesOtherUsersQuotes() {
        var ownerA = UUID.randomUUID();
        var ownerB = UUID.randomUUID();
        insertUser(ownerA);
        insertUser(ownerB);
        repository.save(QuoteMother.draftForOwner(ownerA));
        repository.save(QuoteMother.draftForOwner(ownerB));

        var page = repository.findPage(QuoteQuery.defaults(), ownerA);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).userId()).isEqualTo(ownerA);
    }
}
