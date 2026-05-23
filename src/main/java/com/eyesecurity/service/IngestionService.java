package com.eyesecurity.service;

import com.eyesecurity.common.AnalyticsEvent;
import com.eyesecurity.common.EnrichmentResponse;
import com.eyesecurity.common.IngestionResponse;
import com.eyesecurity.common.SecurityLogRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IngestionService {
    private static final int ANALYTICS_BATCH_SIZE = 20;

    private final ExternalEnrichmentClient enrichmentClient;
    private final ExternalAnalyticsClient analyticsClient;

    public IngestionService(ExternalEnrichmentClient enrichmentClient, ExternalAnalyticsClient analyticsClient) {
        this.enrichmentClient = enrichmentClient;
        this.analyticsClient = analyticsClient;
    }

    public IngestionResponse ingest(List<SecurityLogRecord> records) {
        List<AnalyticsEvent> enrichedEvents = new ArrayList<>();

        for (SecurityLogRecord record : records) {
            EnrichmentResponse enrichment = enrichmentClient.enrich(record);
            enrichedEvents.add(new AnalyticsEvent(
                    record.id(),
                    record.assetName(),
                    record.ip(),
                    enrichment.category(),
                    enrichment.asn(),
                    enrichment.correlationId()
            ));
        }

        int submitted = submitInBatches(enrichedEvents);
        return new IngestionResponse(records.size(), enrichedEvents.size(), submitted);
    }

    private int submitInBatches(List<AnalyticsEvent> events) {
        int submitted = 0;
        for (int start = 0; start < events.size(); start += ANALYTICS_BATCH_SIZE) {
            int end = Math.min(start + ANALYTICS_BATCH_SIZE, events.size());
            analyticsClient.submit(events.subList(start, end));
            submitted += end - start;
        }
        return submitted;
    }
}
