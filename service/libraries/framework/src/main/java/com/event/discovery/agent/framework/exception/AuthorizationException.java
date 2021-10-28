package com.event.discovery.agent.framework.exception;

public class AuthorizationException extends EventDiscoveryAgentException {
    private String userMessage;

    public AuthorizationException(String message, String userMessage) {
        super(DiscoverySupportCode.ACCESS_DENIED_106, message);
        this.userMessage = userMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
