package com.event.discovery.agent.google.api;

import com.event.discovery.agent.google.GooglePubSubSubscriber;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import com.event.discovery.agent.framework.AbstractBrokerPlugin;

import java.util.List;
import java.util.Set;

public interface GooglePubSubApi {

    void configure(String projectId, String credentials);

    List<GooglePubSubSubscriber> bindMessageHandlers(List<Subscription> subscriptionList,
                                                     AbstractBrokerPlugin brokerPlugin);

    void removeSolaceDiscoveryAgentSubscription(Subscription subscription);

    void clearSolaceDiscoveryAgentSubscriptions();

    List<Subscription> createSolaceDiscoveryAgentSubscriptions(Set<String> topicNameSet);

    List<Subscription> getSubscriptionList();

    List<Topic> getTopicList();

    void cleanUp();

}
