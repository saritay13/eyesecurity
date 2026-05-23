package com.eyesecurity.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum SecurityCategory {
    CONTENT_INJECTION("contentinjection"),
    DRIVE_BY_COMPROMISE("drivebycompromise"),
    EXPLOIT_PUBLIC_FACING_APPLICATION("exploitpublicfacingapplication"),
    EXTERNAL_REMOTE_SERVICES("externalremoteservices"),
    HARDWARE_ADDITIONS("hardwareadditions"),
    PHISHING("phishing"),
    REPLICATION_THROUGH_REMOVABLE_MEDIA("replicationthroughremovablemedia"),
    SUPPLY_CHAIN_COMPROMISE("supplychaincompromise"),
    TRUSTED_RELATIONSHIP("trustedrelationship"),
    VALID_ACCOUNTS("validaccounts");

    private final String value;

    SecurityCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static SecurityCategory fromValue(String value) {
        return Arrays.stream(values())
                .filter(category -> category.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported category: " + value));
    }
}
