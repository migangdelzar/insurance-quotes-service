package com.clara.insurancequotes.submission;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.type.QuoteStatusView;
import com.clara.insurancequotes.quote.api.usecase.CreateQuoteUseCase;
import com.clara.insurancequotes.quote.api.usecase.GetOwnedQuoteUseCase;
import com.clara.insurancequotes.quote.api.usecase.UpdateCoverageUseCase;
import com.clara.insurancequotes.submission.api.exception.InsurerUnavailableException;
import com.clara.insurancequotes.submission.api.usecase.SubmitQuoteUseCase;
import com.clara.insurancequotes.testsupport.Containers;
import com.clara.insurancequotes.testsupport.TestUsers;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {"management.opentelemetry.enabled=false", "management.otlp.metrics.export.enabled=false"})
class SubmissionFlowIT {

    private static final WireMockServer INSURER = new WireMockServer(0);

    @BeforeAll
    static void startInsurer() {
        INSURER.start();
    }

    @BeforeEach
    void resetInsurer() {
        INSURER.resetAll();
    }

    @AfterAll
    static void stopInsurer() {
        INSURER.stop();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        Containers.registerPostgres(registry);
        Containers.registerKafka(registry);
        Containers.registerRedis(registry);
        registry.add("insurer.base-url", () -> "http://localhost:" + INSURER.port() + "/submit");
    }

    @Autowired
    private CreateQuoteUseCase createQuoteUseCase;

    @Autowired
    private UpdateCoverageUseCase updateCoverageUseCase;

    @Autowired
    private GetOwnedQuoteUseCase getOwnedQuoteUseCase;

    @Autowired
    private SubmitQuoteUseCase submitQuoteUseCase;

    @Autowired
    private UserRepository users;

    private UUID ownerId;

    @BeforeEach
    void seedOwner() {
        ownerId = TestUsers.create(users);
    }

    private UUID submittableQuote() {
        var id = createQuoteUseCase
                .create(new CreateQuoteCommand("Jane Roe", "jane@example.com", 34, "06600"), ownerId)
                .id();
        updateCoverageUseCase.updateCoverage(
                id, new UpdateCoverageCommand(CoverageType.STANDARD, null, null, null, null, null), ownerId);
        return id;
    }

    @Test
    void successfulSubmission_setsSubmitted_andPublishesKafkaEvent() {
        INSURER.stubFor(post(urlEqualTo("/submit")).willReturn(aResponse().withStatus(200)));
        var id = submittableQuote();

        var view = submitQuoteUseCase.submit(id, ownerId);

        assertThat(view.status()).isEqualTo(QuoteStatusView.SUBMITTED);
        try (var consumer = newConsumer()) {
            consumer.subscribe(List.of("quote-submitted"));
            Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                var records = consumer.poll(Duration.ofMillis(500));
                assertThat(records.records("quote-submitted"))
                        .anySatisfy(record -> assertThat(record.value()).contains(id.toString()));
            });
        }
    }

    @Test
    void failedSubmission_marksFailed_thenRetrySucceeds() {
        INSURER.stubFor(post(urlEqualTo("/submit")).willReturn(aResponse().withStatus(500)));
        var id = submittableQuote();

        assertThatThrownBy(() -> submitQuoteUseCase.submit(id, ownerId))
                .isInstanceOf(InsurerUnavailableException.class);
        assertThat(getOwnedQuoteUseCase.getOwnedQuote(id, ownerId).status())
                .isEqualTo(QuoteStatusView.SUBMISSION_FAILED);

        INSURER.stubFor(post(urlEqualTo("/submit")).willReturn(aResponse().withStatus(200)));
        assertThat(submitQuoteUseCase.submit(id, ownerId).status()).isEqualTo(QuoteStatusView.SUBMITTED);
    }

    @Test
    void submittingTwice_isIdempotent() {
        INSURER.stubFor(post(urlEqualTo("/submit")).willReturn(aResponse().withStatus(200)));
        var id = submittableQuote();

        submitQuoteUseCase.submit(id, ownerId);
        var second = submitQuoteUseCase.submit(id, ownerId);

        assertThat(second.status()).isEqualTo(QuoteStatusView.SUBMITTED);
        assertThat(INSURER.getAllServeEvents()).hasSize(1);
    }

    private KafkaConsumer<String, String> newConsumer() {
        var props = Map.<String, Object>of(
                "bootstrap.servers", Containers.KAFKA.getBootstrapServers(),
                "group.id", "it-" + UUID.randomUUID(),
                "auto.offset.reset", "earliest",
                "key.deserializer", StringDeserializer.class.getName(),
                "value.deserializer", StringDeserializer.class.getName());
        return new KafkaConsumer<>(props);
    }
}
