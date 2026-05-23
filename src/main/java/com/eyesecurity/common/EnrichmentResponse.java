package com.eyesecurity.common;

public record EnrichmentResponse(
        String asn,
        String category,
        long correlationId
) {
}
