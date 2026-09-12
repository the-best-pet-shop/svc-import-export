package com.thebestpetshop.importexport.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum BatchStatus {
    QUEUED, RUNNING, FAILED, SUCCEEDED, APPROVED, ROLLED_BACK;

    @JsonValue
    public String wireValue() { return name().toLowerCase(Locale.ROOT); }

    @JsonCreator
    public static BatchStatus from(String value) {
        return value == null ? null : valueOf(value.toUpperCase(Locale.ROOT));
    }
}
