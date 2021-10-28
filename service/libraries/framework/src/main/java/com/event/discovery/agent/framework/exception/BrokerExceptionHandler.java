package com.event.discovery.agent.framework.exception;

@FunctionalInterface
public interface BrokerExceptionHandler {
    void handleException(Exception exception);
}
