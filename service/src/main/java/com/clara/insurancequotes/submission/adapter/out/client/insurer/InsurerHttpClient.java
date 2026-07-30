package com.clara.insurancequotes.submission.adapter.out.client.insurer;

import com.clara.insurancequotes.submission.api.exception.InsurerUnavailableException;
import com.clara.insurancequotes.submission.application.port.out.InsurerSubmissionPort;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
@Slf4j
public class InsurerHttpClient implements InsurerSubmissionPort {

    private final RestClient restClient;

    @Override
    public void submit(UUID quoteId) {
        try {
            restClient
                    .post()
                    .uri("")
                    .body(Map.of("quoteId", quoteId.toString()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            log.warn("Insurer request failed for quote {}", quoteId, exception);
            throw new InsurerUnavailableException(exception.getMessage());
        }
    }
}
