package com.eyesecurity.service;

import com.eyesecurity.common.AnalyticsEvent;
import com.eyesecurity.common.EnrichmentResponse;
import com.eyesecurity.common.IngestionResponse;
import com.eyesecurity.common.SecurityCategory;
import com.eyesecurity.common.SecurityLogRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionServiceTest {
    private TestEnrichmentClient enrichmentClient;
    private TestAnalyticsClient analyticsClient;
    private IngestionService ingestionService;

    @BeforeEach
    void setUp() {
        enrichmentClient = new TestEnrichmentClient();
        analyticsClient = new TestAnalyticsClient();
        ingestionService = new IngestionService(enrichmentClient, analyticsClient, 0, 0, 0);
    }

    @Test
    void returnsSuccessCountsWhenAllRecordsAreProcessed() {
        List<SecurityLogRecord> records = records(20);

        IngestionResponse response = ingestionService.ingest(records);

        assertThat(response.received()).isEqualTo(20);
        assertThat(response.enriched()).isEqualTo(20);
        assertThat(response.failedEnrichment()).isZero();
        assertThat(response.attemptedAnalytics()).isEqualTo(20);
        assertThat(response.submittedToAnalytics()).isEqualTo(20);
        assertThat(response.failedAnalytics()).isZero();
        assertThat(analyticsClient.batches()).hasSize(1);
    }

    @Test
    void skipsRecordsThatFailEnrichmentAfterRetries() {
        List<SecurityLogRecord> records = records(3);
        enrichmentClient.failForRecordIds(Set.of(2L));

        IngestionResponse response = ingestionService.ingest(records);

        assertThat(response.received()).isEqualTo(3);
        assertThat(response.enriched()).isEqualTo(2);
        assertThat(response.failedEnrichment()).isEqualTo(1);
        assertThat(response.attemptedAnalytics()).isEqualTo(2);
        assertThat(response.submittedToAnalytics()).isEqualTo(2);
        assertThat(response.failedAnalytics()).isZero();
        assertThat(enrichmentClient.attemptsFor(2L)).isEqualTo(3);
    }

    @Test
    void countsAnalyticsRecordsAsFailedWhenSubmissionRetriesAreExhausted() {
        List<SecurityLogRecord> records = records(20);
        analyticsClient.failAllSubmissions();

        IngestionResponse response = ingestionService.ingest(records);

        assertThat(response.received()).isEqualTo(20);
        assertThat(response.enriched()).isEqualTo(20);
        assertThat(response.failedEnrichment()).isZero();
        assertThat(response.attemptedAnalytics()).isEqualTo(20);
        assertThat(response.submittedToAnalytics()).isZero();
        assertThat(response.failedAnalytics()).isEqualTo(20);
        assertThat(analyticsClient.submitAttempts()).isEqualTo(5);
    }

    @Test
    void submitsAnalyticsInBatchesOfTwenty() {
        List<SecurityLogRecord> records = records(45);

        ingestionService.ingest(records);

        assertThat(analyticsClient.batches())
                .extracting(List::size)
                .containsExactly(20, 20, 5);
    }

    private List<SecurityLogRecord> records(int count) {
        return java.util.stream.LongStream.rangeClosed(1, count)
                .mapToObj(id -> new SecurityLogRecord(
                        id,
                        "server_" + id,
                        "10.0.0." + id,
                        LocalDateTime.parse("2024-01-01T00:00:00"),
                        "defender",
                        SecurityCategory.PHISHING
                ))
                .toList();
    }

    private static class TestEnrichmentClient implements EnrichmentClient {
        private final Set<Long> failingRecordIds = ConcurrentHashMap.newKeySet();
        private final ConcurrentHashMap<Long, AtomicInteger> attemptsByRecordId = new ConcurrentHashMap<>();

        @Override
        public EnrichmentResponse enrich(SecurityLogRecord record) {
            attemptsByRecordId.computeIfAbsent(record.id(), ignored -> new AtomicInteger()).incrementAndGet();
            if (failingRecordIds.contains(record.id())) {
                throw new RuntimeException("enrichment unavailable");
            }
            return new EnrichmentResponse("AS" + record.id(), "T1190", record.id() + 1000);
        }

        void failForRecordIds(Set<Long> recordIds) {
            failingRecordIds.addAll(recordIds);
        }

        int attemptsFor(long recordId) {
            return attemptsByRecordId.getOrDefault(recordId, new AtomicInteger()).get();
        }
    }

    private static class TestAnalyticsClient implements AnalyticsClient {
        private final List<List<AnalyticsEvent>> batches = new CopyOnWriteArrayList<>();
        private final AtomicInteger submitAttempts = new AtomicInteger();
        private boolean failAllSubmissions;

        @Override
        public void submit(List<AnalyticsEvent> events) {
            submitAttempts.incrementAndGet();
            if (failAllSubmissions) {
                throw new RuntimeException("rate limited");
            }
            batches.add(new ArrayList<>(events));
        }

        void failAllSubmissions() {
            failAllSubmissions = true;
        }

        List<List<AnalyticsEvent>> batches() {
            return batches;
        }

        int submitAttempts() {
            return submitAttempts.get();
        }
    }
}
