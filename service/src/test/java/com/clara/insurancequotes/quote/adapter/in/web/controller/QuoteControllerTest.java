package com.clara.insurancequotes.quote.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clara.insurancequotes.auth.configuration.JwtConfiguration;
import com.clara.insurancequotes.auth.configuration.SecurityConfiguration;
import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.adapter.in.web.advice.QuoteExceptionHandler;
import com.clara.insurancequotes.quote.api.exception.QuoteNotFoundException;
import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import com.clara.insurancequotes.quote.api.result.QuotePage;
import com.clara.insurancequotes.quote.api.result.QuoteSummary;
import com.clara.insurancequotes.quote.api.type.QuoteStatusView;
import com.clara.insurancequotes.quote.api.type.RequestingUser;
import com.clara.insurancequotes.quote.api.usecase.CreateQuoteUseCase;
import com.clara.insurancequotes.quote.api.usecase.GetQuoteSummaryUseCase;
import com.clara.insurancequotes.quote.api.usecase.GetQuoteUseCase;
import com.clara.insurancequotes.quote.api.usecase.SearchQuotesUseCase;
import com.clara.insurancequotes.quote.api.usecase.UpdateCoverageUseCase;
import com.clara.insurancequotes.quote.domain.exception.HealthDataNotAllowedException;
import com.clara.insurancequotes.shared.configuration.I18nConfiguration;
import com.clara.insurancequotes.shared.error.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QuoteController.class)
@Import({
    SecurityConfiguration.class,
    JwtConfiguration.class,
    I18nConfiguration.class,
    GlobalExceptionHandler.class,
    QuoteExceptionHandler.class
})
@TestPropertySource(properties = {"auth.jwt.secret=test-secret-that-is-32-bytes-long!!"})
class QuoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateQuoteUseCase createQuoteUseCase;

    @MockitoBean
    private UpdateCoverageUseCase updateCoverageUseCase;

    @MockitoBean
    private GetQuoteUseCase getQuoteUseCase;

    @MockitoBean
    private SearchQuotesUseCase searchQuotesUseCase;

    @MockitoBean
    private GetQuoteSummaryUseCase getQuoteSummaryUseCase;

    private static final UUID QUOTE_ID = UUID.fromString("f7d9a1c2-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID = UUID.fromString("b2222222-0000-0000-0000-000000000002");

    private static JwtRequestPostProcessor asOwner() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_api"))
                .jwt(builder -> builder.claim("uid", OWNER_ID.toString()).claim("role", "USER"));
    }

    private static RequestingUser requestingOwner() {
        return new RequestingUser(OWNER_ID, false);
    }

    private static QuoteDetails draftView() {
        return new QuoteDetails(
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
                QuoteStatusView.DRAFT,
                Instant.now(),
                Instant.now());
    }

    @Test
    void createQuote_valid_returns201WithId() throws Exception {
        when(createQuoteUseCase.create(any(), eq(OWNER_ID))).thenReturn(draftView());

        mockMvc.perform(
                        post("/quotes")
                                .with(asOwner())
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
                        .with(asOwner())
                        .contentType("application/json")
                        .content("{\"name\":\"\",\"email\":\"not-an-email\",\"age\":0,\"zipCode\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void updateCoverage_healthDataRejected_returns422() throws Exception {
        when(updateCoverageUseCase.updateCoverage(eq(QUOTE_ID), any(), eq(OWNER_ID)))
                .thenThrow(new HealthDataNotAllowedException(34));

        mockMvc.perform(patch("/quotes/{id}/coverage", QUOTE_ID)
                        .with(asOwner())
                        .contentType("application/json")
                        .content("{\"coverageType\":\"STANDARD\",\"usesTobacco\":true}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("QUOTE_HEALTH_DATA_NOT_ALLOWED"));
    }

    @Test
    void updateCoverage_valid_returnsPremium() throws Exception {
        var view = new QuoteDetails(
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
                QuoteStatusView.DRAFT,
                Instant.now(),
                Instant.now());
        when(updateCoverageUseCase.updateCoverage(eq(QUOTE_ID), any(), eq(OWNER_ID)))
                .thenReturn(view);

        mockMvc.perform(patch("/quotes/{id}/coverage", QUOTE_ID)
                        .with(asOwner())
                        .contentType("application/json")
                        .content("{\"coverageType\":\"STANDARD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPremium").value(100.00));
    }

    @Test
    void getQuote_ownedByOtherUser_returns404() throws Exception {
        when(getQuoteUseCase.getQuote(eq(QUOTE_ID), eq(requestingOwner())))
                .thenThrow(new QuoteNotFoundException(QUOTE_ID));

        mockMvc.perform(get("/quotes/{id}", QUOTE_ID).with(asOwner()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUOTE_NOT_FOUND"));
    }

    @Test
    void getQuotes_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/quotes")).andExpect(status().isUnauthorized());
    }

    @Test
    void unsupportedApiVersion_isRejected() throws Exception {
        mockMvc.perform(get("/quotes").with(asOwner()).header("API-Version", "9.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void supportedApiVersion_routesToController() throws Exception {
        doReturn(new QuotePage(List.of(), 0, 20, 0, 0, false, false))
                .when(searchQuotesUseCase)
                .searchQuotes(any(), eq(requestingOwner()));

        mockMvc.perform(get("/quotes").with(asOwner()).header("API-Version", "1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false));
    }

    @Test
    void listQuotes_acceptsFilteringAndOrderingParameters() throws Exception {
        doReturn(new QuotePage(List.of(), 1, 10, 1, 1, false, true))
                .when(searchQuotesUseCase)
                .searchQuotes(any(), eq(requestingOwner()));

        mockMvc.perform(get("/quotes")
                        .with(asOwner())
                        .header("API-Version", "1.0")
                        .queryParam("page", "1")
                        .queryParam("size", "10")
                        .queryParam("search", "jane")
                        .queryParam("status", "SUBMITTED")
                        .queryParam("coverage", "STANDARD")
                        .queryParam("sortBy", "name")
                        .queryParam("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10));

        var queryCaptor = ArgumentCaptor.forClass(com.clara.insurancequotes.quote.api.query.SearchQuotesQuery.class);
        verify(searchQuotesUseCase).searchQuotes(queryCaptor.capture(), eq(requestingOwner()));
        var query = queryCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(query.page()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(query.size()).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(query.search()).isEqualTo("jane");
        org.assertj.core.api.Assertions.assertThat(query.status()).isEqualTo(QuoteStatusView.SUBMITTED);
        org.assertj.core.api.Assertions.assertThat(query.coverage()).isEqualTo(CoverageType.STANDARD);
        org.assertj.core.api.Assertions.assertThat(query.sortBy().property()).isEqualTo("name");
        org.assertj.core.api.Assertions.assertThat(query.direction().name()).isEqualTo("DESC");
    }

    @Test
    void getQuoteSummary_returnsAnalyticsEnvelope() throws Exception {
        when(getQuoteSummaryUseCase.getSummary(requestingOwner()))
                .thenReturn(new QuoteSummary(
                        3,
                        1,
                        1,
                        1,
                        0,
                        1,
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("50.00"),
                        List.of(),
                        List.of(),
                        List.of()));

        mockMvc.perform(get("/quotes/summary").with(asOwner()).header("API-Version", "1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuotes").value(3))
                .andExpect(jsonPath("$.submissionRate").value(50.00))
                .andExpect(jsonPath("$.trend").isArray());
    }

    @Test
    void listQuotes_invalidQuery_returns400WithQuoteError() throws Exception {
        mockMvc.perform(get("/quotes").with(asOwner()).queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUOTE_INVALID_QUERY"));
    }
}
