package com.event.discovery.agent.google;

import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import com.event.discovery.agent.framework.AbstractBrokerPlugin;
import com.event.discovery.agent.framework.model.GooglePubSubMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class GooglePubSubSubscriberImpl implements GooglePubSubSubscriber {
    private Subscriber subscriber = null;
    private ExecutorProvider executorProvider =
            InstantiatingExecutorProvider.newBuilder().setExecutorThreadCount(1).build();
    private String topicName;
    private AbstractBrokerPlugin brokerPlugin;

    @Override
    public void configure(String topicName, AbstractBrokerPlugin brokerPlugin) {
        this.brokerPlugin = brokerPlugin;
        this.topicName = topicName;
    }

    class MessageReceiver implements com.google.cloud.pubsub.v1.MessageReceiver {

        @Override
        public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
            log.debug("Message Id: {} Data: {}", message.getMessageId(), message.getData().toStringUtf8());

            GooglePubSubMessage gpsMessage = GooglePubSubMessage.builder()
                    .message(message)
                    .receivedTs(System.currentTimeMillis())
                    .topic(topicName)
                    .build();
            brokerPlugin.queueReceivedMessage(gpsMessage);
            consumer.ack();
        }
    }

    /**
     * Receive messages over a subscription.
     */
    @Override
    public void bindReceiver(String projectId, String subscriptionId) {
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(
                projectId, subscriptionId);
        try {
            // create a subscriber bound to the asynchronous message receiver
            subscriber = Subscriber.newBuilder(subscriptionName, new MessageReceiver())
                    .setParallelPullCount(1)
                    .setExecutorProvider(executorProvider)
                    .build();
            subscriber.startAsync().awaitRunning();
        } catch (IllegalStateException e) {
            log.error("Subscriber unexpectedly stopped: ", e);
        }
    }

    @Override
    public void unbindReceiver() {
        if (subscriber != null) {
            subscriber.stopAsync().awaitTerminated();
        }

        // Cleanup threads
        executorProvider.getExecutor().shutdown();
        try {
            if (!executorProvider.getExecutor().awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorProvider.getExecutor().shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("Issue shutting down threads", e);
        } finally {
            executorProvider.getExecutor().shutdownNow();
        }
    }
}
