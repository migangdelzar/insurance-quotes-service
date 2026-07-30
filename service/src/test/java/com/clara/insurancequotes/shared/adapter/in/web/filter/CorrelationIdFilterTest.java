package com.clara.insurancequotes.shared.adapter.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesIdWhenAbsentAndEchoesHeader() throws Exception {
        var response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getHeader("X-Correlation-Id")).isNotBlank();
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void honorsIncomingHeader() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "abc-123");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                assertThat(MDC.get("correlationId")).isEqualTo("abc-123");
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("abc-123");
    }
}
