package com.event.discovery.agent.framework.exception;

public enum DiscoverySupportCode implements SupportCode {

    DISCOVERY_ERROR_101(101, "An error occurred during discovery"),
    VALIDATION_ERROR_102(102, "An invalid request was made"),
    NOT_FOUND_ERROR_103(103, "The resource being requested could not be found"),
    TIMEOUT_104(104, "A timeout occurred while processing the request"),
    UNEXPECTED_ERROR_105(105, "An unexpected error occurred while processing the request"),
    ACCESS_DENIED_106(106, "The resource being requested is not accessible by the caller"),
    SERVICE_UNAVAILABLE_109(109, "A command has been targeted at a service that is unavailable");

    private final int code;
    private final String description;

    DiscoverySupportCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getSupportCode() {
        return String.valueOf(code);
    }

    @Override
    public String getDescription() {
        return description;
    }

}
