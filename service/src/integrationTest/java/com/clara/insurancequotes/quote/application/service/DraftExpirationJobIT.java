package com.clara.insurancequotes.quote.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.usecase.QuoteApi;
import com.clara.insurancequotes.quote.api.usecase.RequestingUser;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import com.clara.insurancequotes.testsupport.Containers;
import com.clara.insurancequotes.testsupport.TestUsers;
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

    @Autowired
    private UserRepository users;

    private UUID ownerId;

    @BeforeEach
    void seedOwner() {
        ownerId = TestUsers.create(users);
    }

    @Test
    void staleDraft_getsExpired_andItsCacheEntryEvicted() {
        var requester = new RequestingUser(ownerId, false);
        var id = quoteApi.create(new CreateQuoteCommand("Jane Roe", "jane@example.com", 34, "06600"), ownerId)
                .id();
        jdbcTemplate.update(
                "update quotes set created_at = ? where id = ?",
                OffsetDateTime.now().minusHours(2),
                id);
        quoteApi.getQuote(id, requester);

        var expired = job.expireStaleDrafts();

        assertThat(expired).isGreaterThanOrEqualTo(1);
        assertThat(cacheManager.getCache("quotes").get(id + "|" + ownerId)).isNull();
        assertThat(quoteApi.getQuote(id, requester).status()).isEqualTo(QuoteStatus.EXPIRED);
    }
}
