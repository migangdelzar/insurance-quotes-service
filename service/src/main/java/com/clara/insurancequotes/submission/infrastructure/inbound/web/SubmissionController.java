package com.clara.insurancequotes.submission.infrastructure.inbound.web;

import com.clara.insurancequotes.quote.api.model.QuoteView;
import com.clara.insurancequotes.submission.api.port.SubmissionApi;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SubmissionController {

    private final SubmissionApi submissionApi;

    @PostMapping("/quotes/{id}/submit")
    public QuoteView submit(@PathVariable UUID id) {
        log.debug("Submitting quote {}", id);
        return submissionApi.submit(id);
    }
}
