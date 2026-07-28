package com.clara.insurancequotes.quote.adapter.in.messaging.consumer;

import com.clara.insurancequotes.quote.configuration.CacheConfig;
import com.clara.insurancequotes.quote.domain.event.QuoteExpired;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuoteCacheEvictionListener {

    private final CacheManager cacheManager;

    @EventListener
    public void onQuoteExpired(QuoteExpired event) {
        var cache = cacheManager.getCache(CacheConfig.QUOTES_CACHE);
        if (cache != null) {
            cache.evict(event.quoteId() + "|" + event.ownerId());
            log.debug("Evicted expired quote {} from cache", event.quoteId());
        }
    }
}
