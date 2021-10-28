package com.event.discovery.agent.google;

import com.event.discovery.agent.framework.AbstractBrokerPlugin;

public interface GooglePubSubSubscriber {
    void configure(String topicName, AbstractBrokerPlugin brokerPlugin);

    void bindReceiver(String projectId, String subscriptionId);

    void unbindReceiver();
}
