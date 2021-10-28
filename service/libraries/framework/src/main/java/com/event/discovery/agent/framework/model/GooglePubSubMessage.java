package com.event.discovery.agent.framework.model;

import com.google.pubsub.v1.PubsubMessage;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GooglePubSubMessage {
    private PubsubMessage message;
    private String topic;
    private long receivedTs;
}
