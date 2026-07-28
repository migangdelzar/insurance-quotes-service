package com.clara.insurancequotes.submission.adapter.out.client.insurer;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class InsurerClientConfig {

    @Bean
    public RestClient insurerRestClient(
            RestClient.Builder builder,
            @Value("${insurer.base-url}") String baseUrl,
            @Value("${insurer.connect-timeout}") Duration connectTimeout,
            @Value("${insurer.read-timeout}") Duration readTimeout) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
