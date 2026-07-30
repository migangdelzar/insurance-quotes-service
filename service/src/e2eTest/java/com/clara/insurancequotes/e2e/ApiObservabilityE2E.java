package com.clara.insurancequotes.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiObservabilityE2E extends ApiE2eSupport {

    @Test
    void healthEndpointReportsTheRealApplicationAsUp() {
        var response = api().getText("/actuator/health");

        assertThat(response.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void prometheusEndpointExposesRuntimeAndHttpMetricsWithoutAuthentication() {
        var response = api().getText("/actuator/prometheus");

        assertThat(response.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains("jvm_memory_used_bytes");
        assertThat(response.body()).contains("http_server_requests");
    }

    @Test
    void quoteEndpointRequiresAuthentication() {
        var response = api().get("/quotes", null);

        assertThat(response.status()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.body().path("code").asText()).isEqualTo("AUTH_REQUIRED");
    }
}
