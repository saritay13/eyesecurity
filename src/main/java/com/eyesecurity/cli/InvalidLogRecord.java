package com.eyesecurity.cli;

public record InvalidLogRecord(
        int lineNumber,
        String rawLine,
        String reason
) {
}
