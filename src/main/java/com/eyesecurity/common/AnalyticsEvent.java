package com.eyesecurity.common;

public record AnalyticsEvent(
        long id,
        String asset,
        String ip,
        String category,
        String asn,
        long correlationId
) {
}
