package com.eyesecurity.cli;

import com.eyesecurity.common.SecurityCategory;
import com.eyesecurity.common.SecurityLogRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecordFiltersTest {
    @Test
    void appliesSourceCategoryAndAssetNameTogether() {
        SecurityLogRecord matchingRecord = record(1, "server_horizon", "defender", SecurityCategory.PHISHING);
        List<SecurityLogRecord> records = List.of(
                matchingRecord,
                record(2, "server_horizon", "crowdstrike", SecurityCategory.PHISHING),
                record(3, "server_horizon", "defender", SecurityCategory.VALID_ACCOUNTS),
                record(4, "server_summit", "defender", SecurityCategory.PHISHING)
        );

        List<SecurityLogRecord> filtered = RecordFilters
                .from("defender", "phising", "server_horizon")
                .apply(records);

        assertThat(filtered).containsExactly(matchingRecord);
    }

    @Test
    void returnsAllRecordsWhenNoFiltersAreProvided() {
        List<SecurityLogRecord> records = List.of(
                record(1, "server_horizon", "defender", SecurityCategory.PHISHING),
                record(2, "server_summit", "crowdstrike", SecurityCategory.VALID_ACCOUNTS)
        );

        assertThat(RecordFilters.from(null, null, null).apply(records)).containsExactlyElementsOf(records);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "phising",
            "Phising",
            "phishing"
    })
    void normalizesIncorrectCategoryFilterValues(String categoryFilter) {
        SecurityLogRecord matchingRecord = record(1, "server_horizon", "defender", SecurityCategory.PHISHING);
        List<SecurityLogRecord> records = List.of(
                matchingRecord,
                record(2, "server_summit", "defender", SecurityCategory.VALID_ACCOUNTS)
        );

        assertThat(RecordFilters.from(null, categoryFilter, null).apply(records)).containsExactly(matchingRecord);
    }

    private SecurityLogRecord record(long id, String assetName, String source, SecurityCategory category) {
        return new SecurityLogRecord(id, assetName, "10.0.0." + id, LocalDateTime.parse("2024-01-01T00:00:00"),
                source, category);
    }
}
