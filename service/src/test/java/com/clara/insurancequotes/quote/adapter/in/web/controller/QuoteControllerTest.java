package com.clara.insurancequotes.quote.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clara.insurancequotes.auth.configuration.JwtConfig;
import com.clara.insurancequotes.auth.configuration.SecurityConfig;
import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.adapter.in.web.advice.QuoteExceptionHandler;
import com.clara.insurancequotes.quote.api.result.QuoteView;
import com.clara.insurancequotes.quote.api.usecase.QuoteApi;
import com.clara.insurancequotes.quote.domain.exception.HealthDataNotAllowedException;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import com.clara.insurancequotes.shared.configuration.I18nConfig;
import com.clara.insurancequotes.shared.error.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QuoteController.class)
@Import({
    SecurityConfig.class,
    JwtConfig.class,
    I18nConfig.class,
    GlobalExceptionHandler.class,
    QuoteExceptionHandler.class
})
@TestPropertySource(properties = {"auth.jwt.secret=test-secret-that-is-32-bytes-long!!"})
class QuoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuoteApi quoteApi;

    private static final UUID QUOTE_ID = UUID.fromString("f7d9a1c2-0000-0000-0000-000000000001");

    private static QuoteView draftView() {
        return new QuoteView(
                QUOTE_ID,
                "Jane Roe",
                "jane@example.com",
                34,
                "06600",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                QuoteStatus.DRAFT,
                Instant.now(),
                Instant.now());
    }

    @Test
    void createQuote_valid_returns201WithId() throws Exception {
        when(quoteApi.create(any())).thenReturn(draftView());

        mockMvc.perform(
                        post("/quotes")
                                .with(jwt())
                                .contentType("application/json")
                                .content(
                                        "{\"name\":\"Jane Roe\",\"email\":\"jane@example.com\",\"age\":34,\"zipCode\":\"06600\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(QUOTE_ID.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void createQuote_missingFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/quotes")
                        .with(jwt())
                        .contentType("application/json")
                        .content("{\"name\":\"\",\"email\":\"not-an-email\",\"age\":0,\"zipCode\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void updateCoverage_healthDataRejected_returns422() throws Exception {
        when(quoteApi.updateCoverage(eq(QUOTE_ID), any())).thenThrow(new HealthDataNotAllowedException(34));

        mockMvc.perform(patch("/quotes/{id}/coverage", QUOTE_ID)
                        .with(jwt())
                        .contentType("application/json")
                        .content("{\"coverageType\":\"STANDARD\",\"usesTobacco\":true}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("QUOTE_HEALTH_DATA_NOT_ALLOWED"));
    }

    @Test
    void updateCoverage_valid_returnsPremium() throws Exception {
        var view = new QuoteView(
                QUOTE_ID,
                "Jane Roe",
                "jane@example.com",
                34,
                "06600",
                CoverageType.STANDARD,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("100.00"),
                QuoteStatus.DRAFT,
                Instant.now(),
                Instant.now());
        when(quoteApi.updateCoverage(eq(QUOTE_ID), any())).thenReturn(view);

        mockMvc.perform(patch("/quotes/{id}/coverage", QUOTE_ID)
                        .with(jwt())
                        .contentType("application/json")
                        .content("{\"coverageType\":\"STANDARD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPremium").value(100.00));
    }

    @Test
    void getQuotes_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/quotes")).andExpect(status().isUnauthorized());
    }
}
