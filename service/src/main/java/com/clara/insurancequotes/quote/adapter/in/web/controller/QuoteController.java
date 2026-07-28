package com.clara.insurancequotes.quote.adapter.in.web.controller;

import com.clara.insurancequotes.quote.adapter.in.web.request.CreateQuoteRequest;
import com.clara.insurancequotes.quote.adapter.in.web.request.UpdateCoverageRequest;
import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.api.result.QuotePageView;
import com.clara.insurancequotes.quote.api.result.QuoteSummaryView;
import com.clara.insurancequotes.quote.api.result.QuoteView;
import com.clara.insurancequotes.quote.api.usecase.QuoteApi;
import com.clara.insurancequotes.quote.api.usecase.RequestingUser;
import com.clara.insurancequotes.quote.application.exception.InvalidQuoteQueryException;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/quotes", version = "1.0")
@RequiredArgsConstructor
@Slf4j
public class QuoteController {

    private static final String ADMIN_ROLE = "ADMIN";

    private final QuoteApi quoteApi;

    @PostMapping
    public ResponseEntity<QuoteView> create(
            @Valid @RequestBody CreateQuoteRequest request, @AuthenticationPrincipal Jwt jwt) {
        var view = quoteApi.create(request.toCommand(), requester(jwt).id());
        log.debug("Created quote response {}", view.id());
        return ResponseEntity.created(URI.create("/quotes/" + view.id())).body(view);
    }

    @PatchMapping("/{id}/coverage")
    public QuoteView updateCoverage(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCoverageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return quoteApi.updateCoverage(id, request.toCommand(), requester(jwt).id());
    }

    @GetMapping("/{id}")
    public QuoteView getQuote(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return quoteApi.getQuote(id, requester(jwt));
    }

    @GetMapping
    public QuotePageView listQuotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String coverage,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            return quoteApi.listQuotes(
                    QuoteQuery.of(page, size, search, status, coverage, sortBy, direction), requester(jwt));
        } catch (IllegalArgumentException exception) {
            throw new InvalidQuoteQueryException(exception.getMessage());
        }
    }

    @GetMapping("/summary")
    public QuoteSummaryView getSummary(@AuthenticationPrincipal Jwt jwt) {
        return quoteApi.getSummary(requester(jwt));
    }

    private static RequestingUser requester(Jwt jwt) {
        return new RequestingUser(
                UUID.fromString(jwt.getClaimAsString("uid")), ADMIN_ROLE.equals(jwt.getClaimAsString("role")));
    }
}
