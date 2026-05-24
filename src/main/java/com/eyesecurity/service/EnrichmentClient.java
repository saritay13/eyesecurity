package com.eyesecurity.service;

import com.eyesecurity.common.EnrichmentResponse;
import com.eyesecurity.common.SecurityLogRecord;

public interface EnrichmentClient {
    EnrichmentResponse enrich(SecurityLogRecord record);
}
