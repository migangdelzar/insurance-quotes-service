package com.clara.insurancequotes.shared.ratelimit;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "web.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private boolean trustForwardedHeaders;
    private boolean failOpen;
    private Bucket auth = new Bucket(10, Duration.ofMinutes(1));
    private Bucket quoteMutation = new Bucket(30, Duration.ofMinutes(1));

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean trustForwardedHeaders() {
        return trustForwardedHeaders;
    }

    public void setTrustForwardedHeaders(boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    public boolean failOpen() {
        return failOpen;
    }

    public void setFailOpen(boolean failOpen) {
        this.failOpen = failOpen;
    }

    public Bucket auth() {
        return auth;
    }

    public void setAuth(Bucket auth) {
        this.auth = auth;
    }

    public Bucket quoteMutation() {
        return quoteMutation;
    }

    public void setQuoteMutation(Bucket quoteMutation) {
        this.quoteMutation = quoteMutation;
    }

    @PostConstruct
    void validate() {
        validateBucket("auth", auth);
        validateBucket("quote-mutation", quoteMutation);
    }

    private static void validateBucket(String name, Bucket bucket) {
        if (bucket == null
                || bucket.limit() <= 0
                || bucket.window() == null
                || bucket.window().isZero()
                || bucket.window().isNegative()) {
            throw new IllegalStateException("web.rate-limit." + name + " must have a positive limit and window");
        }
    }

    public static class Bucket {

        private long limit;
        private Duration window;

        public Bucket() {}

        public Bucket(long limit, Duration window) {
            this.limit = limit;
            this.window = window;
        }

        public long limit() {
            return limit;
        }

        public void setLimit(long limit) {
            this.limit = limit;
        }

        public Duration window() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }
}
