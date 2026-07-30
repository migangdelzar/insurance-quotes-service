package com.clara.insurancequotes.e2e;

import com.clara.insurancequotes.testsupport.Containers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "management.opentelemetry.enabled=false",
            "management.otlp.metrics.export.enabled=false",
            "web.rate-limit.auth.limit=1000",
            "web.rate-limit.quote-mutation.limit=1000"
        })
abstract class ApiE2eSupport {

    protected static final WireMockServer INSURER = new WireMockServer(0);

    static {
        INSURER.start();
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        Containers.registerPostgres(registry);
        Containers.registerKafka(registry);
        Containers.registerRedis(registry);
        registry.add("insurer.base-url", () -> "http://localhost:" + INSURER.port() + "/submit");
    }

    @BeforeEach
    void resetExternalStubs() {
        INSURER.resetAll();
    }

    protected ApiHttpClient api() {
        return new ApiHttpClient(
                RestClient.builder()
                        .baseUrl(URI.create("http://localhost:" + port).toString())
                        .defaultHeader("API-Version", "1.0")
                        .build(),
                objectMapper);
    }

    protected Session login(String username, String password) {
        var response =
                api().postJson("/auth/login", "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        if (response.status() != 200) {
            throw new AssertionError("Login failed with status " + response.status() + ": " + response.body());
        }
        var tokens = response.body().path("tokens");
        return new Session(
                tokens.path("accessToken").asText(), tokens.path("refreshToken").asText());
    }

    protected record ApiResponse(int status, JsonNode body, HttpHeaders headers) {}

    protected record TextResponse(int status, String body, HttpHeaders headers) {}

    protected record Session(String accessToken, String refreshToken) {}

    static final class ApiHttpClient {

        private final RestClient client;
        private final ObjectMapper objectMapper;

        ApiHttpClient(RestClient client, ObjectMapper objectMapper) {
            this.client = client;
            this.objectMapper = objectMapper;
        }

        ApiResponse postJson(String path, String body) {
            return postJson(path, body, null);
        }

        ApiResponse postJson(String path, String body, String accessToken) {
            return client.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> authorize(headers, accessToken))
                    .body(body)
                    .exchange((request, response) -> response(response));
        }

        ApiResponse post(String path, String accessToken) {
            return client.post()
                    .uri(path)
                    .headers(headers -> authorize(headers, accessToken))
                    .exchange((request, response) -> response(response));
        }

        ApiResponse get(String path, String accessToken) {
            return client.get()
                    .uri(path)
                    .headers(headers -> authorize(headers, accessToken))
                    .exchange((request, response) -> response(response));
        }

        TextResponse getText(String path) {
            return client.get().uri(path).exchange((request, response) -> textResponse(response));
        }

        ApiResponse patchJson(String path, String body, String accessToken) {
            return client.patch()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> authorize(headers, accessToken))
                    .body(body)
                    .exchange((request, response) -> response(response));
        }

        private static void authorize(HttpHeaders headers, String accessToken) {
            if (accessToken != null) {
                headers.setBearerAuth(accessToken);
            }
        }

        private ApiResponse response(org.springframework.http.client.ClientHttpResponse response) throws IOException {
            JsonNode body = objectMapper.readTree(response.getBody());
            return new ApiResponse(
                    response.getStatusCode().value(),
                    body == null ? NullNode.getInstance() : body,
                    response.getHeaders());
        }

        private TextResponse textResponse(org.springframework.http.client.ClientHttpResponse response)
                throws IOException {
            return new TextResponse(
                    response.getStatusCode().value(),
                    new String(response.getBody().readAllBytes()),
                    response.getHeaders());
        }
    }
}
