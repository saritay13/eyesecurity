package com.eyesecurity.cli;

import com.eyesecurity.common.SecurityCategory;
import com.eyesecurity.common.SecurityLogRecord;
import com.eyesecurity.service.CategoryNormalizer;

import java.util.List;
import java.util.function.Predicate;

public record RecordFilters(
        String source,
        SecurityCategory category,
        String assetName
) {
    public static RecordFilters from(String source, String category, String assetName) {
        return new RecordFilters(
                blankToNull(source),
                category == null || category.isBlank() ? null : CategoryNormalizer.normalize(category),
                blankToNull(assetName)
        );
    }

    public List<SecurityLogRecord> apply(List<SecurityLogRecord> records) {
        Predicate<SecurityLogRecord> predicate = matchesSource()
                .and(matchesCategory())
                .and(matchesAssetName());

        return records.stream()
                .filter(predicate)
                .toList();
    }

    private Predicate<SecurityLogRecord> matchesSource() {
        return source == null ? record -> true : record -> source.equalsIgnoreCase(record.source());
    }

    private Predicate<SecurityLogRecord> matchesCategory() {
        return category == null ? record -> true : record -> category == record.category();
    }

    private Predicate<SecurityLogRecord> matchesAssetName() {
        return assetName == null ? record -> true : record -> assetName.equalsIgnoreCase(record.assetName());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
