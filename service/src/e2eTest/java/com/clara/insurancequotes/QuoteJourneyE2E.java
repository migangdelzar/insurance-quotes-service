package com.clara.insurancequotes;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.libs.fn.Unchecked;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Supplier;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuoteJourneyE2E {

    private static final String BASE_URL = System.getenv().getOrDefault("E2E_BASE_URL", "http://localhost:8080");
    private static final MediaType JSON = MediaType.get("application/json");
    private static final OkHttpClient CLIENT = new OkHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static String accessToken;
    private static String quoteId;

    private static JsonNode call(Request request) {
        Supplier<JsonNode> execute = Unchecked.supplier(() -> {
            try (Response response = CLIENT.newCall(request).execute()) {
                var responseBody = response.body();
                var body = responseBody == null ? "" : responseBody.string();
                assertThat(response.isSuccessful())
                        .withFailMessage("Unexpected HTTP status %s for %s", response.code(), request.url())
                        .isTrue();
                return MAPPER.readTree(body.isBlank() ? "{}" : body).deepCopy();
            }
        });
        return execute.get();
    }

    private static Request.Builder authorized(String path) {
        return new Request.Builder().url(BASE_URL + path).header("Authorization", "Bearer " + accessToken);
    }

    @Test
    @Order(1)
    void loginIssuesTokens() {
        var request = new Request.Builder()
                .url(BASE_URL + "/auth/login")
                .post(RequestBody.create("{\"username\":\"demo\",\"password\":\"demo-password\"}", JSON))
                .build();

        var body = call(request);

        assertThat(body.path("status").asText()).isEqualTo("TOKENS_ISSUED");
        accessToken = body.path("tokens").path("accessToken").asText();
        assertThat(accessToken).isNotBlank();
    }

    @Test
    @Order(2)
    void unauthenticatedRequestIs401() throws Exception {
        var request = new Request.Builder().url(BASE_URL + "/quotes").build();
        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(401);
        }
    }

    @Test
    @Order(3)
    void createSeniorQuoteThenCoverageMatchesWorkedExample() {
        var created = call(authorized("/quotes")
                .post(RequestBody.create(
                        "{\"name\":\"John Elder\",\"email\":\"john@example.com\",\"age\":70,\"zipCode\":\"06600\"}",
                        JSON))
                .build());
        quoteId = created.path("id").asText();
        assertThat(created.path("status").asText()).isEqualTo("DRAFT");

        var updated = call(authorized("/quotes/" + quoteId + "/coverage")
                .patch(RequestBody.create(
                        "{\"coverageType\":\"STANDARD\",\"hasPreexistingConditions\":true,"
                                + "\"conditions\":[\"DIABETES\"],\"usesTobacco\":true,\"needsSpouseCoverage\":true}",
                        JSON))
                .build());
        assertThat(updated.path("monthlyPremium").decimalValue()).isEqualByComparingTo("327.60");
    }

    @Test
    @Order(4)
    void submitMovesToSubmittedAndIsIdempotent() {
        var empty = RequestBody.create("", null);
        var first =
                call(authorized("/quotes/" + quoteId + "/submit").post(empty).build());
        assertThat(first.path("status").asText()).isEqualTo("SUBMITTED");

        var second =
                call(authorized("/quotes/" + quoteId + "/submit").post(empty).build());
        assertThat(second.path("status").asText()).isEqualTo("SUBMITTED");
    }
}
