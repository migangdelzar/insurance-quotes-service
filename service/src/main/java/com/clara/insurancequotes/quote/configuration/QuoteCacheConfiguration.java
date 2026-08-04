package com.clara.insurancequotes.quote.configuration;

import java.time.Duration;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class QuoteCacheConfiguration {

    public static final String QUOTES_CACHE = "quotes";

    @Configuration
    static class RedisConfiguration {

        @org.springframework.context.annotation.Bean
        RedisCacheManagerBuilderCustomizer quoteCacheConfiguration() {
            var valueSerializer =
                    new GenericJackson2JsonRedisSerializer().configure(mapper -> mapper.findAndRegisterModules());
            var cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(10))
                    .disableCachingNullValues()
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));
            return builder -> builder.withCacheConfiguration(QUOTES_CACHE, cacheConfiguration);
        }
    }
}
