package com.clara.insurancequotes.shared.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RedisCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Quote cache read failed for {}[{}]; falling back to the database", name(cache), key, exception);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Quote cache write failed for {}[{}]", name(cache), key, exception);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Quote cache eviction failed for {}[{}]", name(cache), key, exception);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Quote cache clear failed for {}", name(cache), exception);
    }

    private static String name(Cache cache) {
        return cache == null ? "unknown" : cache.getName();
    }
}
