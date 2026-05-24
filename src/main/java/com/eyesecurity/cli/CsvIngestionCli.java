package com.eyesecurity.cli;

import com.eyesecurity.common.IngestionRequest;
import com.eyesecurity.common.SecurityLogRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;

public class CsvIngestionCli {
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvIngestionCli.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static void main(String[] args) throws Exception {
        CliOptions options = CliOptions.parse(args);
        CsvLogReadResult readResult = CsvLogReader.read(options.csvPath());
        List<SecurityLogRecord> records = readResult.validRecords();

        LOGGER.info("Parsed CSV: {} valid records, {} invalid records",
                readResult.validRecords().size(), readResult.invalidRecords().size());
        readResult.invalidRecords().forEach(invalidRecord -> LOGGER.debug(
                "Invalid CSV row {}: {}. Raw line: {}",
                invalidRecord.lineNumber(),
                invalidRecord.reason(),
                invalidRecord.rawLine()
        ));

        records = options.filters().apply(records);

        HttpResponse<String> response = postToApi(options.apiUrl(), new IngestionRequest(records));
        System.out.printf("Sent %d records to %s%n", records.size(), options.apiUrl());
        System.out.printf("API response: %d %s%n", response.statusCode(), response.body());
    }

    private static HttpResponse<String> postToApi(String apiUrl, IngestionRequest requestBody)
            throws IOException, InterruptedException {
        String json = OBJECT_MAPPER.writeValueAsString(requestBody);
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private record CliOptions(Path csvPath, String apiUrl, RecordFilters filters) {
        private static CliOptions parse(String[] args) {
            Path csvPath = null;
            String apiUrl = "http://localhost:8080/api/ingest";
            String sourceFilter = null;
            String categoryFilter = null;
            String assetNameFilter = null;

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--csv" -> csvPath = Path.of(requireValue(args, ++i, "--csv"));
                    case "--api-url" -> apiUrl = requireValue(args, ++i, "--api-url");
                    case "--source" -> sourceFilter = requireValue(args, ++i, "--source");
                    case "--category" -> categoryFilter = requireValue(args, ++i, "--category");
                    case "--asset-name" -> assetNameFilter = requireValue(args, ++i, "--asset-name");
                    default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
                }
            }

            if (csvPath == null) {
                throw new IllegalArgumentException(
                        "Usage: --csv <path> [--api-url <url>] [--source <source>] "
                                + "[--category <category>] [--asset-name <assetName>]");
            }
            return new CliOptions(csvPath, apiUrl, RecordFilters.from(sourceFilter, categoryFilter, assetNameFilter));
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length || args[index].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }
    }
}
