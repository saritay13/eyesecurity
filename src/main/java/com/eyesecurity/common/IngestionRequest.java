package com.eyesecurity.common;

import java.util.List;

public record IngestionRequest(List<SecurityLogRecord> records) {
}
