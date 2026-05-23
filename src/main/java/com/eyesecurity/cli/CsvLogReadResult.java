package com.eyesecurity.cli;

import com.eyesecurity.common.SecurityLogRecord;

import java.util.List;

public record CsvLogReadResult(
        List<SecurityLogRecord> validRecords,
        List<InvalidLogRecord> invalidRecords
) {
}
