package com.eyesecurity.cli;

import com.eyesecurity.common.SecurityCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CsvLogReaderTest {
    @TempDir
    Path tempDir;

    @Test
    void readsValidRowsAndCollectsInvalidRows() throws Exception {
        Path csv = tempDir.resolve("logs.csv");
        String header = "id;asset_name;ip;created_utc;source;category";
        String validRecord = "119611;server_horizon;102.145.229.227;27/02/2024 00:00;pxtrpf;phising";
        String missingCategoryRecord = "889807;server_summit;131.21.57.124;02/09/2024 00:00;pxtrpf";
        String extraColumnRecord = "123;server_extra;10.0.0.1;01/01/2024 00:00;defender;phishing;extra";
        String invalidIdRecord = "not-a-number;server_bad_id;10.0.0.2;01/01/2024 00:00;defender;phishing";
        String invalidDateRecord = "456;server_bad_date;10.0.0.3;bad-date;defender;phishing";
        Files.writeString(csv, String.join(System.lineSeparator(),
                header,
                validRecord,
                missingCategoryRecord,
                extraColumnRecord,
                invalidIdRecord,
                invalidDateRecord
        ));

        CsvLogReadResult result = CsvLogReader.read(csv);

        assertThat(result.validRecords()).hasSize(1);
        assertThat(result.validRecords().getFirst().category()).isEqualTo(SecurityCategory.PHISHING);
        assertThat(result.invalidRecords()).hasSize(4);
        assertThat(result.invalidRecords())
                .extracting(InvalidLogRecord::lineNumber)
                .containsExactly(3, 4, 5, 6);
    }
}
