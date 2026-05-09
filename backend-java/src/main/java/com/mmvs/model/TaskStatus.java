package com.mmvs.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskStatus {
    WAITING("waiting"),
    RUNNING("running"),
    STREAMING("streaming"),
    FINISHED("finished"),
    FAILED("failed"),
    CANCELLED("cancelled");

    private final String value;

    TaskStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
