package com.eyesecurity.cli;

import com.eyesecurity.common.SecurityLogRecord;
import com.eyesecurity.service.CategoryNormalizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class CsvLogReader {
    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private CsvLogReader() {
    }

    public static CsvLogReadResult read(Path csvPath) throws IOException {
        List<String> lines = Files.readAllLines(csvPath);
        List<SecurityLogRecord> records = new ArrayList<>();
        List<InvalidLogRecord> invalidRecords = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNumber = i + 1;

            try {
                records.add(parseRecord(line, lineNumber));
            } catch (RuntimeException exception) {
                invalidRecords.add(new InvalidLogRecord(lineNumber, line, exception.getMessage()));
            }
        }

        return new CsvLogReadResult(List.copyOf(records), List.copyOf(invalidRecords));
    }

    private static SecurityLogRecord parseRecord(String line, int lineNumber) {
        String[] columns = line.split(";", -1);
        if (columns.length != 6) {
            throw new IllegalArgumentException("expected 6 columns but found " + columns.length);
        }

        return new SecurityLogRecord(
                parseId(columns[0], lineNumber),
                columns[1],
                columns[2],
                LocalDateTime.parse(columns[3], CSV_DATE_FORMAT),
                columns[4],
                CategoryNormalizer.normalize(columns[5])
        );
    }

    private static long parseId(String rawId, int lineNumber) {
        try {
            return Long.parseLong(rawId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("invalid id at line " + lineNumber + ": " + rawId, exception);
        }
    }
}
