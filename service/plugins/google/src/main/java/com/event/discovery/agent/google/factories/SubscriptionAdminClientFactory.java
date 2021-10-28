package com.event.discovery.agent.google.factories;

import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;

import java.io.IOException;

public class SubscriptionAdminClientFactory {
    public SubscriptionAdminClient getSubscriptionAdminClient(SubscriptionAdminSettings subscriptionAdminSettings) throws IOException {
        return SubscriptionAdminClient.create(subscriptionAdminSettings);
    }
}
