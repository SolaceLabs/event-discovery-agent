package com.hivemq.agent.plugin.model;

import lombok.Data;

@Data
public class HiveMQSubscription {
    private String topicFilter;
    private String qos;
    private String retainHandling;
    private boolean retainAsPublished;
    private boolean noLocal;
    private int subscriptionIdentifier;
}
