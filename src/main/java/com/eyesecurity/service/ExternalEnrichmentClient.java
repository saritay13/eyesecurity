package com.eyesecurity.service;

import com.eyesecurity.common.EnrichmentResponse;
import com.eyesecurity.common.SecurityLogRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class ExternalEnrichmentClient {
    private static final String AUTH_HEADER = "eye-am-hiring";

    private final RestClient restClient;

    public ExternalEnrichmentClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://api.heyering.com").build();
    }

    public EnrichmentResponse enrich(SecurityLogRecord record) {
        Map<String, Object> request = Map.of(
                "id", record.id(),
                "asset", record.assetName(),
                "ip", record.ip(),
                "category", record.category().value()
        );

        return restClient.post()
                .uri("/enrichment")
                .header("Authorization", AUTH_HEADER)
                .body(request)
                .retrieve()
                .body(EnrichmentResponse.class);
    }
}
