package com.event.discovery.agent.google;

import com.event.discovery.agent.google.api.GooglePubSubApi;
import com.event.discovery.agent.google.model.GoogleBrokerAuthentication;
import com.google.protobuf.util.Timestamps;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import com.event.discovery.agent.framework.AbstractBrokerPlugin;
import com.event.discovery.agent.framework.VendorBrokerPlugin;
import com.event.discovery.agent.framework.exception.BrokerExceptionHandler;
import com.event.discovery.agent.framework.model.BasicPluginConfigData;
import com.event.discovery.agent.framework.model.BrokerConfigData;
import com.event.discovery.agent.framework.model.GooglePubSubMessage;
import com.event.discovery.agent.framework.model.MessagingClient;
import com.event.discovery.agent.framework.model.NormalizedEvent;
import com.event.discovery.agent.framework.model.PluginConfigData;
import com.event.discovery.agent.framework.model.broker.BasicBrokerIdentity;
import com.event.discovery.agent.framework.model.messages.BasicNormalizedEvent;
import com.event.discovery.agent.framework.model.messages.BasicNormalizedMessage;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GooglePubSubPlugin extends AbstractBrokerPlugin implements VendorBrokerPlugin {

    private final static String PLUGIN_NAME = "GOOGLE";

    private final GooglePubSubApi googlePubSubApi;
    private Map<String, List<MessagingClient>> topicToSubscriptionMap = new HashMap<>();
    private List<Subscription> subscriptionList;
    private List<GooglePubSubSubscriber> subscriberList;

    public GooglePubSubPlugin(MeterRegistry meterRegistry,
                              GooglePubSubApi googlePubSubApi
    ) {
        super(meterRegistry);
        this.googlePubSubApi = googlePubSubApi;
    }

    @Override
    public BrokerConfigData getBrokerConfig() {
        GoogleBrokerAuthentication brokerAuthentication = (GoogleBrokerAuthentication) getBrokerAuthentication();
        String credentials = brokerAuthentication.getCredentials();
        String projectId = brokerAuthentication.getProjectId();
        googlePubSubApi.configure(projectId, credentials);

        // Build the map of topics to subscription before we add our own subscriptions
        populateTopicToSubscriptionMap(googlePubSubApi.getSubscriptionList());

        // Create subscribers for the topic list
        subscriptionList = createMessageListeningTopicSubscriptions(getDiscoveryOperation().getSubscriptionSet());

        return BrokerConfigData.builder()
                .configuration(Collections.emptyMap())
                .build();
    }

    @Override
    public void startSubscribers(final BrokerExceptionHandler brokerExceptionHandler) {
        subscriberList = googlePubSubApi.bindMessageHandlers(subscriptionList, this);
        log.info("Topic subscribers started");
    }

    @Override
    public NormalizedEvent normalizeMessage(Object vendorSpecificMessage) {
        if (vendorSpecificMessage instanceof GooglePubSubMessage) {
            GooglePubSubMessage message = (GooglePubSubMessage) vendorSpecificMessage;
            String topic = message.getTopic();
            return BasicNormalizedEvent.builder()
                    .receiverIds(topicToSubscriptionMap.get(topic))
                    .channelName(topic)
                    .normalizedMessage(
                            BasicNormalizedMessage.builder()
                                    .payload(message.getMessage().getData().toByteArray())
                                    .receivedTimestamp(message.getReceivedTs())
                                    .messageSize(message.getMessage().getSerializedSize())
                                    .sentTimestamp(Timestamps.toMillis(message.getMessage().getPublishTime()))
                                    .build())
                    .build();
        } else {
            log.warn("Unexpected message type", vendorSpecificMessage.getClass().getName());
            return null;
        }
    }

    @Override
    public void stopSubscribers() {
        if (subscriberList != null) {
            for (GooglePubSubSubscriber subscriber : subscriberList) {
                subscriber.unbindReceiver();
            }
        }
        googlePubSubApi.clearSolaceDiscoveryAgentSubscriptions();
    }

    @Override
    public void closeSession() {
        googlePubSubApi.cleanUp();
    }

    private void populateTopicToSubscriptionMap(List<Subscription> subscriptionList) {
        for (Subscription subscription : subscriptionList) {
            String subscriptionName = GooglePubSubUtils.getResourceNameFromResourcePath(subscription.getName());
            String topicName = GooglePubSubUtils.getResourceNameFromResourcePath(subscription.getTopic());
            if (topicToSubscriptionMap.containsKey(topicName)) {
                topicToSubscriptionMap.get(topicName).add(MessagingClient.builder()
                        .name(subscriptionName)
                        .type("client")
                        .build());
            } else {
                List<MessagingClient> subscriptions = new ArrayList<>();
                subscriptions.add(MessagingClient.builder()
                        .name(subscriptionName)
                        .type("client")
                        .build());
                topicToSubscriptionMap.put(topicName, subscriptions);
            }
        }
    }

    private List<Subscription> createMessageListeningTopicSubscriptions(Set<String> subscriptionTopicSet) {
        Set<String> targetedTopics = new HashSet<>();
        if (subscriptionTopicSet == null || subscriptionTopicSet.isEmpty()) {
            // Subscriptions not specified, just do them all
            List<Topic> topicList = googlePubSubApi.getTopicList();
            for (Topic topic : topicList) {
                targetedTopics.add(GooglePubSubUtils.getResourceNameFromResourcePath(topic.getName()));
            }
        } else {
            targetedTopics.addAll(subscriptionTopicSet);
        }
        return googlePubSubApi.createSolaceDiscoveryAgentSubscriptions(targetedTopics);
    }

    @Override
    public PluginConfigData getPluginConfiguration() {
        return BasicPluginConfigData.builder()
                .pluginName(PLUGIN_NAME)
                .brokerCapabilities(getBrokerCapabilities())
                .inputSchema(new HashMap<>())
                .brokerAuthenticationClass(GoogleBrokerAuthentication.class)
                .brokerIdentityClass(BasicBrokerIdentity.class)
                .brokerType("GOOGLE")
                .pluginClass(getClass())
                .build();
    }

}
