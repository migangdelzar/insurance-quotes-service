package com.clara.insurancequotes.quote.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.usecase.RequestingUser;
import com.clara.insurancequotes.quote.application.exception.QuoteNotFoundException;
import com.clara.insurancequotes.testsupport.Containers;
import com.clara.insurancequotes.testsupport.TestUsers;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private UserRepository users;

    private UUID ownerId;

    @BeforeEach
    void seedOwner() {
        ownerId = TestUsers.create(users);
    }

    @Test
    void getQuote_populatesCache_updateCoverageEvicts() {
        var id = service.create(new CreateQuoteCommand("Jane Roe", "jane@example.com", 34, "06600"), ownerId)
                .id();
        var cache = cacheManager.getCache("quotes");
        var owner = new RequestingUser(ownerId, false);

        service.getQuote(id, owner);
        assertThat(redis.keys("*")).isNotEmpty();
        assertThat(cache.get(id + "|" + ownerId)).isNotNull();

        service.updateCoverage(
                id, new UpdateCoverageCommand(CoverageType.BASIC, null, null, null, null, null), ownerId);
        assertThat(cache.get(id + "|" + ownerId)).isNull();
    }

    @Test
    void getQuote_cacheEntryIsIsolatedPerRequester() {
        var id = service.create(new CreateQuoteCommand("Jane Roe", "jane@example.com", 34, "06600"), ownerId)
                .id();
        var admin = new RequestingUser(UUID.randomUUID(), true);
        var otherUser = new RequestingUser(UUID.randomUUID(), false);

        service.getQuote(id, admin);

        assertThatThrownBy(() -> service.getQuote(id, otherUser)).isInstanceOf(QuoteNotFoundException.class);
    }
}
