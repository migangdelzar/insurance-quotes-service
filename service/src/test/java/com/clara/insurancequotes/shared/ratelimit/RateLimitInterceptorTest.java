package com.clara.insurancequotes.shared.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clara.insurancequotes.config.BusinessMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RateLimitInterceptorTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final BusinessMetrics metrics = new BusinessMetrics(registry);
    private final RateLimitProperties properties = new RateLimitProperties();
    private final RateLimiter rateLimiter = Mockito.mock(RateLimiter.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final RateLimitInterceptor interceptor =
            new RateLimitInterceptor(rateLimiter, metrics, properties, objectMapper);

    @Test
    void rateLimitsLoginUsingTheClientAddress() throws Exception {
        var request = request("POST", "/auth/login", "10.0.0.8");
        var response = Mockito.mock(HttpServletResponse.class);
        when(rateLimiter.tryAcquire("auth", "10.0.0.8", 10, Duration.ofMinutes(1)))
                .thenReturn(new RateLimitDecision(true, 10, 9, Duration.ofMinutes(1)));

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();

        verify(response).setHeader("X-RateLimit-Limit", "10");
        verify(response).setHeader("X-RateLimit-Remaining", "9");
        assertThat(registry.get("rate_limit.requests")
                        .tag("bucket", "auth")
                        .tag("outcome", "allowed")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void rejectsQuoteMutationWithRetryHeadersAndApiError() throws Exception {
        var request = request("POST", "/quotes", "10.0.0.8");
        var response = Mockito.mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(Mockito.mock(ServletOutputStream.class));
        when(rateLimiter.tryAcquire("quote_mutation", "10.0.0.8", 30, Duration.ofMinutes(1)))
                .thenReturn(new RateLimitDecision(false, 30, 0, Duration.ofSeconds(12)));

        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();

        verify(response).setStatus(429);
        verify(response).setHeader("Retry-After", "12");
        verify(response).getOutputStream();
        assertThat(registry.get("rate_limit.requests")
                        .tag("bucket", "quote_mutation")
                        .tag("outcome", "rejected")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void failsOpenWhenRedisIsUnavailable() throws Exception {
        var request = request("POST", "/quotes", "10.0.0.8");
        var response = Mockito.mock(HttpServletResponse.class);
        when(rateLimiter.tryAcquire(any(), any(), any(Long.class), any(Duration.class)))
                .thenThrow(new IllegalStateException("redis unavailable"));

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();

        assertThat(registry.get("rate_limit.redis.failures").counter().count()).isEqualTo(1);
    }

    private static HttpServletRequest request(String method, String uri, String remoteAddress) {
        var request = Mockito.mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn(remoteAddress);
        return request;
    }
}
