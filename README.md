# Eye Security Ingestion

Spring Boot ingestion service plus a small Java CLI for reading security logs from CSV and sending them to the API.

## Requirements

- Java 21
- Maven 3.9+

## Setup

Clone the project and compile it:

```bash
git clone https://github.com/saritay13/eyesecurity.git
cd eyesecurity
mvn -DskipTests compile
```

Run tests:

```bash
mvn test
```

## Run The API

Start the ingestion microservice:

```bash
mvn spring-boot:run
```

The API listens on:

```text
POST http://localhost:8080/api/ingest
```

To run on another port:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9081
```

## Run The CLI

The CLI reads a CSV file, validates and normalizes rows, applies optional filters, and posts valid records to the API.

Assumption: the current CLI is designed for small amounts of data. Larger datasets require a redesign

Run with the included CSV:

```bash
mvn exec:java -Dexec.mainClass=com.eyesecurity.cli.CsvIngestionCli -Dexec.args="--csv src/main/resources/example_data_2.csv"
```

Run against a non-default API port:

```bash
mvn exec:java -Dexec.mainClass=com.eyesecurity.cli.CsvIngestionCli -Dexec.args="--csv src/main/resources/example_data_2.csv --api-url http://localhost:9081/api/ingest"
```

Filter by source:

```bash
mvn exec:java -Dexec.mainClass=com.eyesecurity.cli.CsvIngestionCli -Dexec.args="--csv src/main/resources/example_data_2.csv --source defender"
```

Filter by category:

```bash
mvn exec:java -Dexec.mainClass=com.eyesecurity.cli.CsvIngestionCli -Dexec.args="--csv src/main/resources/example_data_2.csv --category phishing"
```

Category filters are normalized, so values like `phising`, `Phising`, `valid accounts`, and `validaccounts` are accepted when they map to a known category.

Filter by asset name:

```bash
mvn exec:java -Dexec.mainClass=com.eyesecurity.cli.CsvIngestionCli -Dexec.args="--csv src/main/resources/example_data_2.csv --asset-name server_horizon"
```

Combine filters:

```bash
mvn exec:java -Dexec.mainClass=com.eyesecurity.cli.CsvIngestionCli -Dexec.args="--csv src/main/resources/example_data_2.csv --source defender --category phishing --asset-name server_horizon"
```

## Ingestion Flow

1. The CLI reads the CSV.
2. Valid rows are converted into `SecurityLogRecord` objects.
3. Invalid CSV rows are stored separately and logged:
   - wrong column count
   - missing category
   - invalid id
   - invalid date
   - unsupported category
4. Optional CLI filters are applied:
   - `source`
   - `category`
   - `asset-name`
5. The CLI sends only valid, filtered records to `/api/ingest`.
6. The API enriches records in batches of 20.
7. Enrichment failures are retried and then counted without failing the whole request.
8. Successfully enriched records are submitted to analytics in batches.
9. Analytics calls respect the external limit of 1 request per 10 seconds.
10. Analytics failures are retried with exponential backoff and counted if retries are exhausted.

## API Response

The API returns counts for each stage:

```json
{
  "received": 997,
  "enriched": 995,
  "failedEnrichment": 2,
  "attemptedAnalytics": 995,
  "submittedToAnalytics": 995,
  "failedAnalytics": 0
}
```

Field meanings:

- `received`: records received by the API after CLI parsing/filtering
- `enriched`: records successfully enriched
- `failedEnrichment`: records skipped because enrichment failed after retries
- `attemptedAnalytics`: enriched records attempted for analytics submission
- `submittedToAnalytics`: records successfully submitted to analytics
- `failedAnalytics`: records that failed analytics submission after retries

## Notes

- Analytics is rate-limited to 1 request per 10 seconds, so ingesting the full CSV can take several minutes.
- Enrichment is retried with a short delay because the external service may have intermittent failures.
- Analytics is retried with exponential backoff because rate limiting can still happen around timing boundaries.
