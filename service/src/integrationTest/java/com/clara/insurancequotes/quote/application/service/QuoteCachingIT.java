package com.clara.insurancequotes.quote.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.testsupport.Containers;
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

    @Test
    void getQuote_populatesCache_updateCoverageEvicts() {
        var id = service.create(new CreateQuoteCommand("Jane Roe", "jane@example.com", 34, "06600"))
                .id();
        var cache = cacheManager.getCache("quotes");

        service.getQuote(id);
        assertThat(redis.keys("*")).isNotEmpty();
        assertThat(cache.get(id)).isNotNull();

        service.updateCoverage(id, new UpdateCoverageCommand(CoverageType.BASIC, null, null, null, null, null));
        assertThat(cache.get(id)).isNull();
    }
}
