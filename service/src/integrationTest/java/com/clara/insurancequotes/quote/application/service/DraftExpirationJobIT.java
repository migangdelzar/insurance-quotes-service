package com.clara.insurancequotes.quote.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.usecase.QuoteApi;
import com.clara.insurancequotes.quote.api.usecase.RequestingUser;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import com.clara.insurancequotes.testsupport.Containers;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {"spring.kafka.bootstrap-servers=localhost:1"})
class DraftExpirationJobIT {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        Containers.registerPostgres(registry);
        Containers.registerRedis(registry);
    }

    @Autowired
    private QuoteApi quoteApi;

    @Autowired
    private DraftExpirationJob job;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    private static final UUID OWNER = UUID.randomUUID();

    @BeforeEach
    void seedOwner() {
        jdbcTemplate.update(
                "insert into users (id, username, password_hash, role, created_at) values (?, ?, ?, ?, ?) "
                        + "on conflict (id) do nothing",
                OWNER,
                "user-" + OWNER,
                "hash",
                "USER",
                Timestamp.from(Instant.now()));
    }

    @Test
    void staleDraft_getsExpired_andItsCacheEntryEvicted() {
        var requester = new RequestingUser(OWNER, false);
        var id = quoteApi.create(new CreateQuoteCommand("Jane Roe", "jane@example.com", 34, "06600"), OWNER)
                .id();
        jdbcTemplate.update(
                "update quotes set created_at = ? where id = ?",
                OffsetDateTime.now().minusHours(2),
                id);
        quoteApi.getQuote(id, requester);

        var expired = job.expireStaleDrafts();

        assertThat(expired).isGreaterThanOrEqualTo(1);
        assertThat(cacheManager.getCache("quotes").get(id + "|" + OWNER)).isNull();
        assertThat(quoteApi.getQuote(id, requester).status()).isEqualTo(QuoteStatus.EXPIRED);
    }
}
