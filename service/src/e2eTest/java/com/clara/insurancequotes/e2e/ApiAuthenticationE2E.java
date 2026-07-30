package com.clara.insurancequotes.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiAuthenticationE2E extends ApiE2eSupport {

    @Test
    void passwordLoginIssuesAccessAndRefreshTokens() {
        var response = api().postJson("/auth/login", "{\"username\":\"demo\",\"password\":\"demo-password\"}");

        assertThat(response.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body().path("status").asText()).isEqualTo("TOKENS_ISSUED");
        assertThat(response.body().path("tokens").path("accessToken").asText()).isNotBlank();
        assertThat(response.body().path("tokens").path("refreshToken").asText()).isNotBlank();
    }

    @Test
    void invalidPasswordReturnsProblemDetails() {
        var response = api().postJson("/auth/login", "{\"username\":\"demo\",\"password\":\"wrong-password\"}");

        assertThat(response.status()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        JsonNode body = response.body();
        assertThat(body.path("status").asInt()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(body.path("code").asText()).isEqualTo("AUTH_INVALID_CREDENTIALS");
        assertThat(body.path("message").asText()).isNotBlank();
    }

    @Test
    void refreshRotatesTokenAndRejectsThePreviousRefreshToken() {
        var session = login("demo-two", "demo-password-two");

        var rotated = api().postJson("/auth/refresh", "{\"refreshToken\":\"" + session.refreshToken() + "\"}");

        assertThat(rotated.status()).isEqualTo(HttpStatus.OK.value());
        var rotatedRefreshToken = rotated.body().path("refreshToken").asText();
        assertThat(rotatedRefreshToken).isNotBlank().isNotEqualTo(session.refreshToken());

        var reused = api().postJson("/auth/refresh", "{\"refreshToken\":\"" + session.refreshToken() + "\"}");

        assertThat(reused.status()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(reused.body().path("code").asText()).isEqualTo("AUTH_INVALID_REFRESH_TOKEN");
    }

    @Test
    void logoutRevokesRefreshToken() {
        var session = login("demo-three", "demo-password-three");

        var logout = api().postJson("/auth/logout", "{\"refreshToken\":\"" + session.refreshToken() + "\"}");

        assertThat(logout.status()).isEqualTo(HttpStatus.NO_CONTENT.value());
        var refresh = api().postJson("/auth/refresh", "{\"refreshToken\":\"" + session.refreshToken() + "\"}");
        assertThat(refresh.status()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void passkeyRegistrationOptionsAreAvailableAfterPasswordLogin() {
        var session = login("demo", "demo-password");

        var response = api().post("/auth/webauthn/register-options", session.accessToken());

        assertThat(response.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body().path("challengeId").asText()).isNotBlank();
        assertThat(response.body().path("publicKeyOptionsJson").asText()).isNotBlank();
    }

    @Test
    void passwordlessAssertionForUnregisteredUserReturnsActionableError() {
        var response = api().postJson("/auth/webauthn/assertion-options", "{\"username\":\"demo-three\"}");

        assertThat(response.status()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.body().path("code").asText()).isEqualTo("AUTH_PASSKEY_NOT_REGISTERED");
        assertThat(response.body().path("message").asText()).isNotBlank();
    }
}
