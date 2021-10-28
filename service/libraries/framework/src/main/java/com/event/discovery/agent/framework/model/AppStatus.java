package com.event.discovery.agent.framework.model;

import lombok.Getter;

public enum AppStatus {
    RUNNING("running"),
    ERROR("error"),
    UNKNOWN("unknown"),
    COMPLETED("completed");

    @Getter
    private String value;

    AppStatus(String value) {
        this.value = value;
    }
}
