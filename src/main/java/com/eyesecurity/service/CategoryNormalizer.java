package com.eyesecurity.service;

import com.eyesecurity.common.SecurityCategory;

import java.util.Map;

public final class CategoryNormalizer {
    private static final Map<String, SecurityCategory> KNOWN_FIXES = Map.ofEntries(
            Map.entry("content injection", SecurityCategory.CONTENT_INJECTION),
            Map.entry("content_injection", SecurityCategory.CONTENT_INJECTION),
            Map.entry("drive by compromise", SecurityCategory.DRIVE_BY_COMPROMISE),
            Map.entry("drive-by-compromise", SecurityCategory.DRIVE_BY_COMPROMISE),
            Map.entry("compromise driveby", SecurityCategory.DRIVE_BY_COMPROMISE),
            Map.entry("explaoit-public facing", SecurityCategory.EXPLOIT_PUBLIC_FACING_APPLICATION),
            Map.entry("exploit public facing", SecurityCategory.EXPLOIT_PUBLIC_FACING_APPLICATION),
            Map.entry("external remote service", SecurityCategory.EXTERNAL_REMOTE_SERVICES),
            Map.entry("external-remote-service", SecurityCategory.EXTERNAL_REMOTE_SERVICES),
            Map.entry("hardware additions", SecurityCategory.HARDWARE_ADDITIONS),
            Map.entry("hardware-additions", SecurityCategory.HARDWARE_ADDITIONS),
            Map.entry("hardware_additions", SecurityCategory.HARDWARE_ADDITIONS),
            Map.entry("phising", SecurityCategory.PHISHING),
            Map.entry("replication through removable media", SecurityCategory.REPLICATION_THROUGH_REMOVABLE_MEDIA),
            Map.entry("replication-through-removable-media", SecurityCategory.REPLICATION_THROUGH_REMOVABLE_MEDIA),
            Map.entry("supply chain compromise", SecurityCategory.SUPPLY_CHAIN_COMPROMISE),
            Map.entry("supply_chain_compromise", SecurityCategory.SUPPLY_CHAIN_COMPROMISE),
            Map.entry("trusted relationship", SecurityCategory.TRUSTED_RELATIONSHIP),
            Map.entry("trusted-relationship", SecurityCategory.TRUSTED_RELATIONSHIP),
            Map.entry("valid accounts", SecurityCategory.VALID_ACCOUNTS),
            Map.entry("valid-accounts", SecurityCategory.VALID_ACCOUNTS),
            Map.entry("valida_accounts", SecurityCategory.VALID_ACCOUNTS)
    );

    private CategoryNormalizer() {
    }

    public static SecurityCategory normalize(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank() || "none".equalsIgnoreCase(rawCategory)) {
            throw new IllegalArgumentException("Category is required");
        }

        String key = rawCategory.trim()
                .toLowerCase()
                .replace("(", "")
                .replace(")", "");

        SecurityCategory category = KNOWN_FIXES.get(key);
        if (category != null) {
            return category;
        }

        return SecurityCategory.fromValue(key.replace("-", "").replace("_", "").replace(" ", ""));
    }
}
