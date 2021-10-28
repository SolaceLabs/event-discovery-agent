package com.event.discovery.agent.framework.exception;

public class EventDiscoveryAgentException extends RuntimeException {
    private final SupportCode supportCode;

    public EventDiscoveryAgentException(SupportCode supportCode) {
        super();
        this.supportCode = supportCode;
    }

    public EventDiscoveryAgentException(SupportCode supportCode, String message) {
        super(message);
        this.supportCode = supportCode;
    }

    public EventDiscoveryAgentException(SupportCode supportCode, String message, Exception cause) {
        super(message, cause);
        this.supportCode = supportCode;
    }

    public EventDiscoveryAgentException(SupportCode supportCode, Exception cause) {
        super(cause);
        this.supportCode = supportCode;
    }

    public SupportCode getSupportCode() {
        return supportCode;
    }
}

