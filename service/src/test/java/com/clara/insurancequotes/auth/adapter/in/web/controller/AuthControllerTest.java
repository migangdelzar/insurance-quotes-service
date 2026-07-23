package com.clara.insurancequotes.auth.adapter.in.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clara.insurancequotes.auth.adapter.in.web.advice.AuthExceptionHandler;
import com.clara.insurancequotes.auth.application.service.TokenService;
import com.clara.insurancequotes.auth.configuration.JwtConfig;
import com.clara.insurancequotes.auth.configuration.SecurityConfig;
import com.clara.insurancequotes.shared.configuration.I18nConfig;
import com.clara.insurancequotes.shared.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({
    SecurityConfig.class,
    JwtConfig.class,
    TokenService.class,
    I18nConfig.class,
    GlobalExceptionHandler.class,
    AuthExceptionHandler.class
})
@TestPropertySource(
        properties = {
            "auth.jwt.secret=test-secret-that-is-32-bytes-long!!",
            "auth.jwt.ttl=30m",
            "auth.demo.username=demo",
            "auth.demo.password=demo-password"
        })
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validCredentials_returnAccessToken() throws Exception {
        mockMvc.perform(post("/auth/token")
                        .contentType("application/json")
                        .content("{\"username\":\"demo\",\"password\":\"demo-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresInSeconds").value(1800));
    }

    @Test
    void wrongCredentials_return401ApiError() throws Exception {
        mockMvc.perform(post("/auth/token")
                        .contentType("application/json")
                        .content("{\"username\":\"demo\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void protectedEndpointWithoutToken_returns401ApiErrorShape() throws Exception {
        mockMvc.perform(get("/quotes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }
}
