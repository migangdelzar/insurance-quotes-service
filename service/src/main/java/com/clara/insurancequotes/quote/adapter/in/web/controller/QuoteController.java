package com.clara.insurancequotes.quote.adapter.in.web.controller;

import com.clara.insurancequotes.quote.adapter.in.web.exception.InvalidQuoteQueryException;
import com.clara.insurancequotes.quote.adapter.in.web.request.CreateQuoteRequest;
import com.clara.insurancequotes.quote.adapter.in.web.request.UpdateCoverageRequest;
import com.clara.insurancequotes.quote.api.query.SearchQuotesQuery;
import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import com.clara.insurancequotes.quote.api.result.QuotePage;
import com.clara.insurancequotes.quote.api.result.QuoteSummary;
import com.clara.insurancequotes.quote.api.type.RequestingUser;
import com.clara.insurancequotes.quote.api.usecase.CreateQuoteUseCase;
import com.clara.insurancequotes.quote.api.usecase.GetQuoteSummaryUseCase;
import com.clara.insurancequotes.quote.api.usecase.GetQuoteUseCase;
import com.clara.insurancequotes.quote.api.usecase.SearchQuotesUseCase;
import com.clara.insurancequotes.quote.api.usecase.UpdateCoverageUseCase;
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

    private final CreateQuoteUseCase createQuoteUseCase;
    private final UpdateCoverageUseCase updateCoverageUseCase;
    private final GetQuoteUseCase getQuoteUseCase;
    private final SearchQuotesUseCase searchQuotesUseCase;
    private final GetQuoteSummaryUseCase getQuoteSummaryUseCase;

    @PostMapping
    public ResponseEntity<QuoteDetails> create(
            @Valid @RequestBody CreateQuoteRequest request, @AuthenticationPrincipal Jwt jwt) {
        var view = createQuoteUseCase.create(request.toCommand(), requester(jwt).id());
        log.debug("Created quote response {}", view.id());
        return ResponseEntity.created(URI.create("/quotes/" + view.id())).body(view);
    }

    @PatchMapping("/{id}/coverage")
    public QuoteDetails updateCoverage(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCoverageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return updateCoverageUseCase.updateCoverage(
                id, request.toCommand(), requester(jwt).id());
    }

    @GetMapping("/{id}")
    public QuoteDetails getQuote(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return getQuoteUseCase.getQuote(id, requester(jwt));
    }

    @GetMapping
    public QuotePage listQuotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String coverage,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal Jwt jwt) {
        final SearchQuotesQuery query;
        try {
            query = SearchQuotesQuery.of(page, size, search, status, coverage, sortBy, direction);
        } catch (IllegalArgumentException exception) {
            throw new InvalidQuoteQueryException(exception.getMessage(), exception);
        }
        return searchQuotesUseCase.searchQuotes(query, requester(jwt));
    }

    @GetMapping("/summary")
    public QuoteSummary getSummary(@AuthenticationPrincipal Jwt jwt) {
        return getQuoteSummaryUseCase.getSummary(requester(jwt));
    }

    private static RequestingUser requester(Jwt jwt) {
        return new RequestingUser(
                UUID.fromString(jwt.getClaimAsString("uid")), ADMIN_ROLE.equals(jwt.getClaimAsString("role")));
    }
}
