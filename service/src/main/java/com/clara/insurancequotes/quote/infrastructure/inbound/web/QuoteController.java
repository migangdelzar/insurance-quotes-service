package com.clara.insurancequotes.quote.infrastructure.inbound.web;

import com.clara.insurancequotes.quote.api.model.QuoteView;
import com.clara.insurancequotes.quote.api.port.QuoteApi;
import com.clara.insurancequotes.quote.infrastructure.inbound.web.model.CreateQuoteRequest;
import com.clara.insurancequotes.quote.infrastructure.inbound.web.model.UpdateCoverageRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quotes")
@RequiredArgsConstructor
@Slf4j
public class QuoteController {

    private final QuoteApi quoteApi;

    @PostMapping
    public ResponseEntity<QuoteView> create(@Valid @RequestBody CreateQuoteRequest request) {
        var view = quoteApi.create(request.toCommand());
        log.debug("Created quote response {}", view.id());
        return ResponseEntity.created(URI.create("/quotes/" + view.id())).body(view);
    }

    @PatchMapping("/{id}/coverage")
    public QuoteView updateCoverage(@PathVariable UUID id, @Valid @RequestBody UpdateCoverageRequest request) {
        return quoteApi.updateCoverage(id, request.toCommand());
    }

    @GetMapping("/{id}")
    public QuoteView getQuote(@PathVariable UUID id) {
        return quoteApi.getQuote(id);
    }

    @GetMapping
    public List<QuoteView> listQuotes() {
        return quoteApi.listQuotes();
    }
}
