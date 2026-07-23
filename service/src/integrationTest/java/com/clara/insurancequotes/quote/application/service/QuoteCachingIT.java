package com.clara.insurancequotes.quote.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.pricing.api.model.CoverageType;
import com.clara.insurancequotes.quote.api.model.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.model.UpdateCoverageCommand;
import com.clara.insurancequotes.testsupport.Containers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {"spring.kafka.bootstrap-servers=localhost:1"})
class QuoteCachingIT {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        Containers.registerPostgres(registry);
    }

    @Autowired
    private QuoteService service;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void getQuote_populatesCache_updateCoverageEvicts() {
        var id = service.create(new CreateQuoteCommand("Jane Roe", "jane@example.com", 34, "06600"))
                .id();
        var cache = cacheManager.getCache("quotes");

        service.getQuote(id);
        assertThat(cache.get(id)).isNotNull();

        service.updateCoverage(id, new UpdateCoverageCommand(CoverageType.BASIC, null, null, null, null, null));
        assertThat(cache.get(id)).isNull();
    }
}
