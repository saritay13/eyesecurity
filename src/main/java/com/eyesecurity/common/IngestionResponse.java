package com.eyesecurity.common;

public record IngestionResponse(
        int received,
        int enriched,
        int submittedToAnalytics
) {
}
