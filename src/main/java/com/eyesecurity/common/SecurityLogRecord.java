package com.eyesecurity.common;

import java.time.LocalDateTime;

public record SecurityLogRecord(
        long id,
        String assetName,
        String ip,
        LocalDateTime createdUtc,
        String source,
        SecurityCategory category
) {
}
