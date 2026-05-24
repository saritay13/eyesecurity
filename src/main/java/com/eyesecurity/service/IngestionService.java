package com.eyesecurity.service;

import com.eyesecurity.common.AnalyticsEvent;
import com.eyesecurity.common.EnrichmentResponse;
import com.eyesecurity.common.IngestionResponse;
import com.eyesecurity.common.SecurityLogRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class IngestionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestionService.class);
    private static final int ANALYTICS_BATCH_SIZE = 20;
    private static final int ENRICHMENT_MAX_ATTEMPTS = 3;
    // Assumption: enrichment failures are intermittent, so a short retry pause is enough without adding much latency.
    private static final long DEFAULT_ENRICHMENT_RETRY_DELAY_MILLIS = 200;
    private static final int ANALYTICS_MAX_ATTEMPTS = 5;
    private static final long DEFAULT_ANALYTICS_RETRY_DELAY_MILLIS = 1_000;
    private static final long DEFAULT_ANALYTICS_RATE_LIMIT_DELAY_MILLIS = 10_000;

    private final EnrichmentClient enrichmentClient;
    private final AnalyticsClient analyticsClient;
    private final long enrichmentRetryDelayMillis;
    private final long analyticsRetryDelayMillis;
    private final long analyticsRateLimitDelayMillis;

    public IngestionService(EnrichmentClient enrichmentClient, AnalyticsClient analyticsClient) {
        this(enrichmentClient,
                analyticsClient,
                DEFAULT_ENRICHMENT_RETRY_DELAY_MILLIS,
                DEFAULT_ANALYTICS_RETRY_DELAY_MILLIS,
                DEFAULT_ANALYTICS_RATE_LIMIT_DELAY_MILLIS);
    }

    IngestionService(
            EnrichmentClient enrichmentClient,
            AnalyticsClient analyticsClient,
            long enrichmentRetryDelayMillis,
            long analyticsRetryDelayMillis,
            long analyticsRateLimitDelayMillis
    ) {
        this.enrichmentClient = enrichmentClient;
        this.analyticsClient = analyticsClient;
        this.enrichmentRetryDelayMillis = enrichmentRetryDelayMillis;
        this.analyticsRetryDelayMillis = analyticsRetryDelayMillis;
        this.analyticsRateLimitDelayMillis = analyticsRateLimitDelayMillis;
    }

    public IngestionResponse ingest(List<SecurityLogRecord> records) {
        List<SecurityLogRecord> failedEnrichmentRecords = new ArrayList<>();
        int enriched = 0;
        int attemptedAnalytics = 0;
        int submittedToAnalytics = 0;
        int failedAnalytics = 0;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int start = 0; start < records.size(); start += ANALYTICS_BATCH_SIZE) {
                int end = Math.min(start + ANALYTICS_BATCH_SIZE, records.size());
                int batchNumber = (start / ANALYTICS_BATCH_SIZE) + 1;
                List<EnrichmentResult> results = enrichBatch(records.subList(start, end), executor);
                List<AnalyticsEvent> analyticsBatch = new ArrayList<>();

                for (EnrichmentResult result : results) {
                    if (result.event() != null) {
                        analyticsBatch.add(result.event());
                    } else {
                        failedEnrichmentRecords.add(result.record());
                        LOGGER.debug("Failed to enrich record {} after {} attempts",
                                result.record().id(), ENRICHMENT_MAX_ATTEMPTS, result.failure());
                    }
                }

                enriched += analyticsBatch.size();
                if (!analyticsBatch.isEmpty()) {
                    attemptedAnalytics += analyticsBatch.size();
                    try {
                        LOGGER.info("Sending analytics batch {} with {} records", batchNumber, analyticsBatch.size());
                        submitToAnalyticsWithRetry(analyticsBatch);
                        submittedToAnalytics += analyticsBatch.size();
                        LOGGER.info("Successfully sent analytics batch {} with {} records",
                                batchNumber, analyticsBatch.size());
                    } catch (RuntimeException exception) {
                        failedAnalytics += analyticsBatch.size();
                        LOGGER.warn("Failed to submit analytics batch containing {} records after {} attempts",
                                analyticsBatch.size(), ANALYTICS_MAX_ATTEMPTS, exception);
                    }
                    waitForAnalyticsRateLimit(start, records.size());
                }
            }
        }

        LOGGER.info("Enrichment completed: {} received records, {} enriched records, {} failed enrichment records",
                records.size(), enriched, failedEnrichmentRecords.size());
        LOGGER.info("Analytics completed: {} attempted records, {} submitted records, {} failed analytics records",
                attemptedAnalytics, submittedToAnalytics, failedAnalytics);

        return new IngestionResponse(records.size(), enriched, failedEnrichmentRecords.size(),
                attemptedAnalytics, submittedToAnalytics, failedAnalytics);
    }

    private List<EnrichmentResult> enrichBatch(List<SecurityLogRecord> records, ExecutorService executor) {
        List<CompletableFuture<EnrichmentResult>> futures = records.stream()
                .map(record -> CompletableFuture.supplyAsync(() -> enrichRecord(record), executor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private EnrichmentResult enrichRecord(SecurityLogRecord record) {
        try {
            EnrichmentResponse enrichment = enrichRecordWithRetry(record);
            return new EnrichmentResult(record, new AnalyticsEvent(
                    record.id(),
                    record.assetName(),
                    record.ip(),
                    enrichment.category(),
                    enrichment.asn(),
                    enrichment.correlationId()
            ), null);
        } catch (RuntimeException exception) {
            return new EnrichmentResult(record, null, exception);
        }
    }

    private EnrichmentResponse enrichRecordWithRetry(SecurityLogRecord record) {
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= ENRICHMENT_MAX_ATTEMPTS; attempt++) {
            try {
                return enrichmentClient.enrich(record);
            } catch (RuntimeException exception) {
                lastException = exception;
                if (attempt < ENRICHMENT_MAX_ATTEMPTS) {
                    LOGGER.debug("Enrichment attempt {} failed for record {}; retrying",
                            attempt, record.id(), exception);
                    sleepBeforeRetry(record, attempt);
                }
            }
        }

        throw new IllegalStateException("Failed to enrich record " + record.id()
                + " after " + ENRICHMENT_MAX_ATTEMPTS + " attempts", lastException);
    }

    private void sleepBeforeRetry(SecurityLogRecord record, int attempt) {
        try {
            Thread.sleep(enrichmentRetryDelayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying enrichment for record " + record.id()
                    + " after attempt " + attempt, exception);
        }
    }

    private void submitToAnalyticsWithRetry(List<AnalyticsEvent> events) {
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= ANALYTICS_MAX_ATTEMPTS; attempt++) {
            try {
                analyticsClient.submit(events);
                return;
            } catch (RuntimeException exception) {
                lastException = exception;
                if (attempt < ANALYTICS_MAX_ATTEMPTS) {
                    long delayMillis = analyticsRetryDelayMillis(attempt);
                    LOGGER.warn("Analytics submission attempt {} failed for batch of {}; retrying in {} ms",
                            attempt, events.size(), delayMillis, exception);
                    sleepBeforeAnalyticsRetry(attempt);
                }
            }
        }

        throw new IllegalStateException("Failed to submit analytics batch with " + events.size()
                + " records after " + ANALYTICS_MAX_ATTEMPTS + " attempts", lastException);
    }

    private void sleepBeforeAnalyticsRetry(int attempt) {
        try {
            Thread.sleep(analyticsRetryDelayMillis(attempt));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying analytics submission after attempt "
                    + attempt, exception);
        }
    }

    private long analyticsRetryDelayMillis(int attempt) {
        return analyticsRetryDelayMillis * (1L << (attempt - 1));
    }

    private void waitForAnalyticsRateLimit(int batchStart, int totalRecords) {
        if (batchStart + ANALYTICS_BATCH_SIZE >= totalRecords) {
            return;
        }

        try {
            Thread.sleep(analyticsRateLimitDelayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for analytics rate limit", exception);
        }
    }

    private record EnrichmentResult(
            SecurityLogRecord record,
            AnalyticsEvent event,
            RuntimeException failure
    ) {
    }
}
