package com.clara.insurancequotes.auth.adapter.in.web.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clara.insurancequotes.auth.adapter.in.web.advice.AuthExceptionHandler;
import com.clara.insurancequotes.auth.api.exception.InvalidCredentialsException;
import com.clara.insurancequotes.auth.api.exception.InvalidRefreshTokenException;
import com.clara.insurancequotes.auth.api.exception.PasskeyNotRegisteredException;
import com.clara.insurancequotes.auth.api.result.LoginResponse;
import com.clara.insurancequotes.auth.api.result.TokenPairResponse;
import com.clara.insurancequotes.auth.api.usecase.AssertPasskeyUseCase;
import com.clara.insurancequotes.auth.api.usecase.LoginUseCase;
import com.clara.insurancequotes.auth.api.usecase.LogoutUseCase;
import com.clara.insurancequotes.auth.api.usecase.RefreshTokenUseCase;
import com.clara.insurancequotes.auth.api.usecase.RegisterPasskeyUseCase;
import com.clara.insurancequotes.auth.api.usecase.StartPasskeyAssertionUseCase;
import com.clara.insurancequotes.auth.api.usecase.StartPasskeyRegistrationUseCase;
import com.clara.insurancequotes.auth.configuration.JwtConfig;
import com.clara.insurancequotes.auth.configuration.SecurityConfig;
import com.clara.insurancequotes.shared.configuration.I18nConfig;
import com.clara.insurancequotes.shared.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({
    SecurityConfig.class,
    JwtConfig.class,
    I18nConfig.class,
    GlobalExceptionHandler.class,
    AuthExceptionHandler.class
})
@TestPropertySource(
        properties = {
            "auth.jwt.secret=test-secret-that-is-32-bytes-long!!",
            "auth.jwt.ttl=30m",
            "auth.demo.username=demo",
            "auth.demo.password=demo-password",
            "web.cors.allowed-origins=http://localhost:5173,http://localhost:3000,http://localhost:3100"
        })
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockitoBean
    private LogoutUseCase logoutUseCase;

    @MockitoBean
    private AssertPasskeyUseCase assertPasskeyUseCase;

    @MockitoBean
    private RegisterPasskeyUseCase registerPasskeyUseCase;

    @MockitoBean
    private StartPasskeyAssertionUseCase startPasskeyAssertionUseCase;

    @MockitoBean
    private StartPasskeyRegistrationUseCase startPasskeyRegistrationUseCase;

    @Test
    void validCredentials_returnTokenPair() throws Exception {
        when(loginUseCase.login("demo", "demo-password"))
                .thenReturn(LoginResponse.tokensIssued(new TokenPairResponse("access", "refresh", 1800)));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"demo\",\"password\":\"demo-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TOKENS_ISSUED"))
                .andExpect(jsonPath("$.tokens.accessToken").value("access"));
    }

    @Test
    void wrongCredentials_return401ApiError() throws Exception {
        when(loginUseCase.login("demo", "nope")).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"demo\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void invalidRefreshToken_returns401ApiError() throws Exception {
        when(refreshTokenUseCase.refresh("stale-refresh-token")).thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"stale-refresh-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_REFRESH_TOKEN"));
    }

    @Test
    void passkeyAssertionForUnregisteredUser_returnsSetupError() throws Exception {
        when(startPasskeyAssertionUseCase.startAssertion("demo")).thenThrow(new PasskeyNotRegisteredException());

        mockMvc.perform(post("/auth/webauthn/assertion-options")
                        .contentType("application/json")
                        .content("{\"username\":\"demo\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_PASSKEY_NOT_REGISTERED"));
    }

    @Test
    void protectedEndpointWithoutToken_returns401ApiErrorShape() throws Exception {
        mockMvc.perform(get("/quotes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void frontendOrigin_allowsApiVersionHeader() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,api-version"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("api-version")));
    }

    @Test
    void insuranceFrontendOrigin_allowsApiVersionHeader() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://localhost:3100")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,api-version"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3100"))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("api-version")));
    }
}
