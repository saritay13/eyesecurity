package com.eyesecurity.service;

import com.eyesecurity.common.AnalyticsEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ExternalAnalyticsClient {
    private static final String AUTH_HEADER = "eye-am-hiring";

    private final RestClient restClient;

    public ExternalAnalyticsClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://api.heyering.com").build();
    }

    public void submit(List<AnalyticsEvent> events) {
        restClient.post()
                .uri("/analytics")
                .header("Authorization", AUTH_HEADER)
                .body(events)
                .retrieve()
                .toBodilessEntity();
    }
}
