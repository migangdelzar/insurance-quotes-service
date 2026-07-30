package com.clara.insurancequotes.e2e;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiQuoteJourneysE2E extends ApiE2eSupport {

    @Test
    void userCanCreatePriceReadAndSearchQuote() {
        var session = login("demo", "demo-password");
        var created = createQuote(session, "Alice Lifecycle");

        assertThat(created.status()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(created.body().path("status").asText()).isEqualTo("DRAFT");
        UUID quoteId = UUID.fromString(created.body().path("id").asText());

        var updated = api().patchJson(
                        "/quotes/" + quoteId + "/coverage", "{\"coverageType\":\"STANDARD\"}", session.accessToken());

        assertThat(updated.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(updated.body().path("coverageType").asText()).isEqualTo("STANDARD");
        assertThat(updated.body().path("monthlyPremium").decimalValue()).isPositive();

        var read = api().get("/quotes/" + quoteId, session.accessToken());
        assertThat(read.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(read.body().path("id").asText()).isEqualTo(quoteId.toString());
        assertThat(read.body().path("monthlyPremium").decimalValue())
                .isEqualByComparingTo(updated.body().path("monthlyPremium").decimalValue());

        var search = api().get(
                        "/quotes?search=Alice+Lifecycle&status=DRAFT&coverage=STANDARD&size=5&sortBy=createdAt&direction=asc",
                        session.accessToken());
        assertThat(search.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(search.body().path("content"))
                .as(search.body().toPrettyString())
                .hasSize(1);
        assertThat(search.body().path("content").get(0).path("id").asText()).isEqualTo(quoteId.toString());
    }

    @Test
    void userCannotReadOrMutateAnotherUsersQuote() {
        var owner = login("demo", "demo-password");
        var otherUser = login("demo-two", "demo-password-two");
        var created = createQuote(owner, "Private Quote");
        UUID quoteId = UUID.fromString(created.body().path("id").asText());

        var read = api().get("/quotes/" + quoteId, otherUser.accessToken());
        var update = api().patchJson(
                        "/quotes/" + quoteId + "/coverage", "{\"coverageType\":\"BASIC\"}", otherUser.accessToken());

        assertThat(read.status()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(read.body().path("code").asText()).isEqualTo("QUOTE_NOT_FOUND");
        assertThat(update.status()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void adminCanSeeQuoteSummaryAcrossUsers() {
        var owner = login("demo", "demo-password");
        var admin = login("demo-admin", "demo-admin-password");
        createQuote(owner, "Admin Summary Quote");

        var summary = api().get("/quotes/summary", admin.accessToken());
        var list = api().get("/quotes?size=1&sortBy=createdAt&direction=desc", admin.accessToken());

        assertThat(summary.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(summary.body().path("totalQuotes").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(list.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(list.body().path("totalElements").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(list.body().path("size").asInt()).isEqualTo(1);
    }

    @Test
    void invalidQuotePayloadReturnsFieldValidationErrors() {
        var session = login("demo-three", "demo-password-three");

        var response = api().postJson(
                        "/quotes",
                        "{\"name\":\"\",\"email\":\"not-an-email\",\"age\":17,\"zipCode\":\"1\"}",
                        session.accessToken());

        assertThat(response.status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.body().path("code").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.body().path("fieldErrors")).isNotEmpty();
    }

    @Test
    void invalidPagingReturnsAnActionableQueryError() {
        var session = login("demo-three", "demo-password-three");

        var response = api().get("/quotes?page=-1&size=20", session.accessToken());

        assertThat(response.status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.body().path("code").asText()).isEqualTo("QUOTE_INVALID_QUERY");
        assertThat(response.body().path("message").asText()).isNotBlank();
    }

    @Test
    void healthDataForNonSeniorApplicantIsRejected() {
        var session = login("demo", "demo-password");
        var created = createQuote(session, "Health Rule Quote");
        UUID quoteId = UUID.fromString(created.body().path("id").asText());

        var response = api().patchJson(
                        "/quotes/" + quoteId + "/coverage",
                        "{\"coverageType\":\"PREMIUM\",\"hasPreexistingConditions\":true,"
                                + "\"conditions\":[\"DIABETES\"],\"takesPrescriptionMedication\":true,"
                                + "\"usesTobacco\":false,\"needsSpouseCoverage\":false}",
                        session.accessToken());

        assertThat(response.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(response.body().path("code").asText()).isEqualTo("QUOTE_HEALTH_DATA_NOT_ALLOWED");
    }

    @Test
    void submissionSuccessFailureAndRetryAreExposedThroughRest() {
        var session = login("demo", "demo-password");
        var created = createQuote(session, "Submission Quote");
        UUID quoteId = UUID.fromString(created.body().path("id").asText());
        updateToStandard(quoteId, session);

        INSURER.stubFor(post(urlEqualTo("/submit")).willReturn(aResponse().withStatus(500)));
        var failed = api().post("/quotes/" + quoteId + "/submit", session.accessToken());
        assertThat(failed.status()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(failed.body().path("code").asText()).isEqualTo("INSURER_UNAVAILABLE");

        INSURER.stubFor(post(urlEqualTo("/submit")).willReturn(aResponse().withStatus(200)));
        var submitted = api().post("/quotes/" + quoteId + "/submit", session.accessToken());
        var duplicate = api().post("/quotes/" + quoteId + "/submit", session.accessToken());

        assertThat(submitted.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(submitted.body().path("status").asText()).isEqualTo("SUBMITTED");
        assertThat(duplicate.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(duplicate.body().path("status").asText()).isEqualTo("SUBMITTED");
        assertThat(INSURER.getAllServeEvents()).hasSize(2);
    }

    @Test
    void incompleteQuoteCannotBeSubmitted() {
        var session = login("demo-two", "demo-password-two");
        var created = createQuote(session, "Incomplete Submission Quote");
        UUID quoteId = UUID.fromString(created.body().path("id").asText());

        var response = api().post("/quotes/" + quoteId + "/submit", session.accessToken());

        assertThat(response.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(response.body().path("code").asText()).isEqualTo("QUOTE_INCOMPLETE");
    }

    private ApiResponse createQuote(Session session, String name) {
        return api().postJson(
                        "/quotes",
                        "{\"name\":\"" + name + "\",\"email\":\""
                                + name.toLowerCase().replace(' ', '.')
                                + "@example.com\",\"age\":34,\"zipCode\":\"06600\"}",
                        session.accessToken());
    }

    private void updateToStandard(UUID quoteId, Session session) {
        var response = api().patchJson(
                        "/quotes/" + quoteId + "/coverage", "{\"coverageType\":\"STANDARD\"}", session.accessToken());
        assertThat(response.status()).isEqualTo(HttpStatus.OK.value());
    }
}
