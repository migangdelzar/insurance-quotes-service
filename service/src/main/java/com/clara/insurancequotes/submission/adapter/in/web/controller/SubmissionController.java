package com.clara.insurancequotes.submission.adapter.in.web.controller;

import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import com.clara.insurancequotes.submission.api.usecase.SubmissionApi;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SubmissionController {

    private final SubmissionApi submissionApi;

    @PostMapping(value = "/quotes/{id}/submit", version = "1.0")
    public QuoteDetails submit(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        log.debug("Submitting quote {}", id);
        return submissionApi.submit(id, UUID.fromString(jwt.getClaimAsString("uid")));
    }
}
