package com.clara.insurancequotes.quote.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.usecase.RequestingUser;
import com.clara.insurancequotes.quote.application.exception.QuoteNotFoundException;
import com.clara.insurancequotes.testsupport.Containers;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {"spring.kafka.bootstrap-servers=localhost:1"})
class QuoteCachingIT {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        Containers.registerPostgres(registry);
        Containers.registerRedis(registry);
    }

    @Autowired
    private QuoteService service;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void getQuote_populatesCache_updateCoverageEvicts() {
        var id = service.create(new CreateQuoteCommand("Jane Roe", "jane@example.com", 34, "06600"), OWNER)
                .id();
        var cache = cacheManager.getCache("quotes");
        var owner = new RequestingUser(OWNER, false);

        service.getQuote(id, owner);
        assertThat(redis.keys("*")).isNotEmpty();
        assertThat(cache.get(id + "|" + OWNER)).isNotNull();

        service.updateCoverage(id, new UpdateCoverageCommand(CoverageType.BASIC, null, null, null, null, null), OWNER);
        assertThat(cache.get(id + "|" + OWNER)).isNull();
    }

    @Test
    void getQuote_cacheEntryIsIsolatedPerRequester() {
        var id = service.create(new CreateQuoteCommand("Jane Roe", "jane@example.com", 34, "06600"), OWNER)
                .id();
        var admin = new RequestingUser(UUID.randomUUID(), true);
        var otherUser = new RequestingUser(UUID.randomUUID(), false);

        service.getQuote(id, admin);

        assertThatThrownBy(() -> service.getQuote(id, otherUser)).isInstanceOf(QuoteNotFoundException.class);
    }
}
