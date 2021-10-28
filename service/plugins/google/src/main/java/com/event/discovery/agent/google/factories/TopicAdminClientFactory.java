package com.event.discovery.agent.google.factories;

import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;

import java.io.IOException;

public class TopicAdminClientFactory {
    public TopicAdminClient getTopicAdminClient(TopicAdminSettings topicAdminSettings) throws IOException {
        return TopicAdminClient.create(topicAdminSettings);
    }
}
