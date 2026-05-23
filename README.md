# Eye Security Ingestion

Simple technical-assessment structure with two entry points:

- `com.eyesecurity.api.AnalyticsIngestionApplication`: Spring Boot microservice API.
- `com.eyesecurity.cli.CsvIngestionCli`: plain Java CLI that reads a CSV and posts it to the API.

## API

Run the microservice:

```bash
mvn spring-boot:run
```

Endpoint:

```text
POST http://localhost:8080/api/ingest
```

Body:

```json
{
  "records": [
    {
      "id": 119611,
      "assetName": "server_horizon",
      "ip": "102.145.229.227",
      "createdUtc": "2024-02-27T00:00:00",
      "source": "pxtrpf",
      "category": "exploitpublicfacingapplication"
    }
  ]
}
```

## CLI

Compile:

```bash
mvn -DskipTests compile
```

Run:

```bash
mvn exec:java \
  -Dexec.mainClass=com.eyesecurity.cli.CsvIngestionCli \
  -Dexec.args="--csv /path/to/example_data_2.csv"
```

Optional source filter:

```bash
mvn exec:java \
  -Dexec.mainClass=com.eyesecurity.cli.CsvIngestionCli \
  -Dexec.args="--csv /path/to/example_data_2.csv --source defender"
```

## Assumptions

- The CLI sends all parsed records to the local microservice in one request.
- The microservice calls the external enrichment service once per record.
- Analytics requests are batched with a maximum of 20 events per request.
- Retry/backoff and stronger validation are intentionally left as next steps to keep this skeleton small.
