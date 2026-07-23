package com.clara.insurancequotes.submission.adapter.out.client.insurer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.clara.insurancequotes.submission.api.exception.InsurerUnavailableException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpstatInsurerClientTest {

    private static final String BASE_URL = "https://insurer.test/submit";

    private MockRestServiceServer server;
    private HttpstatInsurerClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpstatInsurerClient(builder.baseUrl(BASE_URL).build());
    }

    @Test
    void acceptedResponse_completesSilently() {
        server.expect(requestTo(BASE_URL)).andExpect(method(HttpMethod.POST)).andRespond(withSuccess());

        assertThatCode(() -> client.submit(UUID.randomUUID())).doesNotThrowAnyException();
    }

    @Test
    void serverError_translatesToInsurerUnavailable() {
        server.expect(requestTo(BASE_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.submit(UUID.randomUUID())).isInstanceOf(InsurerUnavailableException.class);
    }
}
