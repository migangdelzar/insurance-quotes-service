package com.clara.insurancequotes.shared.ratelimit;

import com.clara.insurancequotes.config.BusinessMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnBean(RateLimiter.class)
public class RateLimitConfiguration implements WebMvcConfigurer {

    private final RateLimitInterceptor interceptor;

    public RateLimitConfiguration(
            RateLimiter rateLimiter,
            BusinessMetrics metrics,
            RateLimitProperties properties,
            ObjectMapper objectMapper) {
        this.interceptor = new RateLimitInterceptor(rateLimiter, metrics, properties, objectMapper);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor);
    }
}
