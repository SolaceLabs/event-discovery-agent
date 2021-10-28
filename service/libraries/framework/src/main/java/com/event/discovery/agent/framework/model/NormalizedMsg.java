package com.event.discovery.agent.framework.model;

public interface NormalizedMsg {
    byte[] getPayload();

    String getContentType();

    long getMessageSize();

    long getSentTimestamp();

    long getReceivedTimestamp();
}
