package com.eyesecurity.common;

public record IngestionResponse(
        int received,
        int enriched,
        int failedEnrichment,
        int attemptedAnalytics,
        int submittedToAnalytics,
        int failedAnalytics
) {
}
