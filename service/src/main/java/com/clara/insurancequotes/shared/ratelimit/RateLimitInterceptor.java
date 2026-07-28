package com.clara.insurancequotes.shared.ratelimit;

import com.clara.insurancequotes.config.BusinessMetrics;
import com.clara.insurancequotes.shared.error.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;
    private final BusinessMetrics metrics;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(
            RateLimiter rateLimiter,
            BusinessMetrics metrics,
            RateLimitProperties properties,
            ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!properties.enabled() || !"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        var bucket = bucket(request.getRequestURI());
        if (bucket == null) {
            return true;
        }

        var clientKey = clientKey(request);
        RateLimitDecision decision;
        try {
            decision = rateLimiter.tryAcquire(
                    bucket.name(),
                    clientKey,
                    bucket.config().limit(),
                    bucket.config().window());
        } catch (RuntimeException exception) {
            metrics.rateLimiterRedisFailure();
            if (properties.failOpen()) {
                log.warn("Redis rate limiter unavailable; allowing request for bucket {}", bucket.name(), exception);
                return true;
            }
            log.error("Redis rate limiter unavailable; rejecting request for bucket {}", bucket.name(), exception);
            response.setStatus(503);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(
                    response.getOutputStream(),
                    ApiError.of(503, "RATE_LIMIT_UNAVAILABLE", "Request protection is temporarily unavailable."));
            return false;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        if (decision.allowed()) {
            metrics.rateLimitAllowed(bucket.name());
            return true;
        }

        metrics.rateLimitRejected(bucket.name());
        var retryAfterSeconds = Math.max(1, (decision.retryAfter().toMillis() + 999) / 1000);
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(), ApiError.of(429, "RATE_LIMITED", "Too many requests. Try again later."));
        return false;
    }

    private Bucket bucket(String uri) {
        if (uri == null) {
            return null;
        }
        if (uri.equals("/auth/login")
                || uri.equals("/auth/webauthn/assertion-options")
                || uri.equals("/auth/webauthn/assert")) {
            return new Bucket("auth", properties.auth());
        }
        if (uri.equals("/quotes") || uri.matches("/quotes/[^/]+/(coverage|submit)")) {
            return new Bucket("quote_mutation", properties.quoteMutation());
        }
        return null;
    }

    private String clientKey(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal != null
                && principal.getName() != null
                && !principal.getName().isBlank()) {
            return "user:" + principal.getName();
        }
        if (properties.trustForwardedHeaders()) {
            var forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",", 2)[0].trim();
            }
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private record Bucket(String name, RateLimitProperties.Bucket config) {}
}
