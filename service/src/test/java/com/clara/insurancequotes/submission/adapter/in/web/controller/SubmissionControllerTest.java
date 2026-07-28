package com.clara.insurancequotes.submission.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clara.insurancequotes.auth.configuration.JwtConfig;
import com.clara.insurancequotes.auth.configuration.SecurityConfig;
import com.clara.insurancequotes.quote.application.exception.QuoteNotFoundException;
import com.clara.insurancequotes.shared.configuration.I18nConfig;
import com.clara.insurancequotes.shared.error.GlobalExceptionHandler;
import com.clara.insurancequotes.submission.adapter.in.web.advice.SubmissionExceptionHandler;
import com.clara.insurancequotes.submission.api.usecase.SubmissionApi;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SubmissionController.class)
@Import({
    SecurityConfig.class,
    JwtConfig.class,
    I18nConfig.class,
    GlobalExceptionHandler.class,
    SubmissionExceptionHandler.class
})
@TestPropertySource(properties = {"auth.jwt.secret=test-secret-that-is-32-bytes-long!!"})
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubmissionApi submissionApi;

    private static final UUID QUOTE_ID = UUID.fromString("f7d9a1c2-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID = UUID.fromString("b2222222-0000-0000-0000-000000000002");

    @Test
    void submit_onAnotherUsersQuote_returns404() throws Exception {
        when(submissionApi.submit(eq(QUOTE_ID), eq(OWNER_ID))).thenThrow(new QuoteNotFoundException(QUOTE_ID));

        mockMvc.perform(post("/quotes/{id}/submit", QUOTE_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_api"))
                                .jwt(builder -> builder.claim("uid", OWNER_ID.toString()).claim("role", "USER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUOTE_NOT_FOUND"));
    }
}
