package com.event.discovery.agent.kafkaCommon.plugin;

import com.event.discovery.agent.framework.AbstractBrokerPlugin;
import com.event.discovery.agent.framework.VendorBrokerPlugin;
import com.event.discovery.agent.framework.exception.BrokerExceptionHandler;
import com.event.discovery.agent.framework.exception.DiscoverySupportCode;
import com.event.discovery.agent.framework.exception.EventDiscoveryAgentException;
import com.event.discovery.agent.framework.model.BrokerConfigData;
import com.event.discovery.agent.framework.model.NormalizedEvent;
import com.event.discovery.agent.kafkaCommon.messageConsumer.ApacheKafkaMessageConsumer;
import com.event.discovery.agent.kafkaCommon.messageConsumer.ApacheKafkaSnapshotMessageConsumer;
import com.event.discovery.agent.kafkaCommon.model.messages.KafkaNormalizedEventImpl;
import com.event.discovery.agent.kafkaCommon.model.messages.KafkaNormalizedMessageImpl;
import com.event.discovery.agent.kafkaCommon.KafkaCommon;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaAuthentication;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaIdentity;
import com.event.discovery.agent.kafkaCommon.model.RawKafkaRecord;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractApacheKafkaPlugin extends AbstractBrokerPlugin implements VendorBrokerPlugin {
    private final ObjectProvider<KafkaConsumer<byte[], byte[]>> kafkaConsumerObjectProvider;
    protected final KafkaCommon kafkaCommon;
    private final CountDownLatch latch = new CountDownLatch(1);

    private ApacheKafkaMessageConsumer apacheKafkaMessageConsumer;

    abstract protected String getPluginName();

    public AbstractApacheKafkaPlugin(ObjectProvider<KafkaConsumer<byte[], byte[]>> kafkaConsumerObjectProvider,
                                     MeterRegistry meterRegistry,
                                     KafkaCommon kafkaCommon) {
        super(meterRegistry);
        this.kafkaConsumerObjectProvider = kafkaConsumerObjectProvider;
        this.kafkaCommon = kafkaCommon;
    }

    @Override
    public BrokerConfigData getBrokerConfig() {
        kafkaCommon.initialize((ApacheKafkaIdentity) getBrokerIdentity(),
                (ApacheKafkaAuthentication) getBrokerAuthentication(),
                getDiscoveryOperation());
        kafkaCommon.createAdminClient();
        kafkaCommon.createConnectorClient();

        try {
            return kafkaCommon.buildKafkaDiscoveryData();
        } catch (IOException | ExecutionException | InterruptedException e) {
            log.error("Error getting kafka subscriptions", e);
            throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101,
                    "Error getting kafka consumers", e);
        }
    }

    @Override
    public void startSubscribers(final BrokerExceptionHandler brokerExceptionHandler) {

        // Get list of topics
        try {
            List<String> topicList = kafkaCommon.getTopicList();
            List<String> subscribingTopics = new ArrayList<>();
            if (CollectionUtils.isEmpty(getDiscoveryOperation().getSubscriptionSet())) {
                subscribingTopics.addAll(topicList);
            } else {
                subscribingTopics.addAll(topicList
                        .stream()
                        .filter(topicName -> getDiscoveryOperation().getSubscriptionSet().contains(topicName))
                        .collect(Collectors.toList()));
            }

            // Create the consumer
            apacheKafkaMessageConsumer = new ApacheKafkaSnapshotMessageConsumer(kafkaConsumerObjectProvider,
                    subscribingTopics, (ApacheKafkaIdentity) getBrokerIdentity(), (ApacheKafkaAuthentication) getBrokerAuthentication(), this,
                    latch, brokerExceptionHandler, kafkaCommon);
            apacheKafkaMessageConsumer.start();

            // Wait for the consumer to subscribe. Otherwise the application starts the timer right away
            // when we return and this reduces the amount of time we listen for messages.
            latch.await();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public NormalizedEvent normalizeMessage(Object vendorSpecificMessage) {
        if (vendorSpecificMessage instanceof RawKafkaRecord) {
            RawKafkaRecord message = (RawKafkaRecord) vendorSpecificMessage;
            String topic = message.getTopic();
            int length = message.getValue() != null ? message.getValue().length : 0;
            return KafkaNormalizedEventImpl.builder()
                    .keySchema(null)
                    .channelName(topic)
                    .channelType("topic")
                    .fromSchemaRegistry(false)
                    .normalizedMessage(KafkaNormalizedMessageImpl.builder()
                            .key((message.getKey() != null) ? message.getKey() : new byte[0])
                            .payload(message.getValue())
                            .receivedTimestamp(message.getReceivedTimestamp())
                            .sentTimestamp(message.getCreatedTimestamp())
                            .messageSize(length)
                            .build())
                    .build();
        } else {
            log.warn("Unexpected message type", vendorSpecificMessage.getClass().getName());
            return null;
        }
    }

    @Override
    public void stopSubscribers() {
        if (apacheKafkaMessageConsumer != null) {
            apacheKafkaMessageConsumer.stop();
        }
    }

    @Override
    public void closeSession() {
        kafkaCommon.cleanup();
    }
}
