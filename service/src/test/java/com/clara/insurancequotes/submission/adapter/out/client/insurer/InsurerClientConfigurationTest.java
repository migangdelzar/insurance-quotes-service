package com.clara.insurancequotes.submission.adapter.out.client.insurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class InsurerClientConfigurationTest {

    @Test
    void exposesRestClientBuilderForSpringBootFour() {
        assertThat(new InsurerClientConfiguration().restClientBuilder()).isNotNull();
    }

    @Test
    void usesReusableJdkHttpClientFactoryForInsurerCalls() {
        var builder = mock(RestClient.Builder.class);
        var client = mock(RestClient.class);
        when(builder.baseUrl("http://insurer")).thenReturn(builder);
        when(builder.requestFactory(any(ClientHttpRequestFactory.class))).thenReturn(builder);
        when(builder.build()).thenReturn(client);

        var result = new InsurerClientConfiguration()
                .insurerRestClient(builder, "http://insurer", Duration.ofSeconds(2), Duration.ofSeconds(5));

        var factory = ArgumentCaptor.forClass(ClientHttpRequestFactory.class);
        verify(builder).requestFactory(factory.capture());
        assertThat(factory.getValue()).isInstanceOf(JdkClientHttpRequestFactory.class);
        assertThat(result).isSameAs(client);
    }
}
