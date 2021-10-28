package com.event.discovery.agent.kafkaCommon.messageConsumer;

import com.event.discovery.agent.framework.AbstractBrokerPlugin;
import com.event.discovery.agent.framework.exception.BrokerExceptionHandler;
import com.event.discovery.agent.kafkaCommon.KafkaCommon;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaAuthentication;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaIdentity;
import com.event.discovery.agent.kafkaCommon.model.RawKafkaRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

@Slf4j
public abstract class ApacheKafkaMessageConsumer {
    protected final ObjectProvider<KafkaConsumer<byte[], byte[]>> kafkaConsumerObjectProvider;
    protected final List<String> topicList;
    protected final ApacheKafkaIdentity brokerIdentity;
    protected final ApacheKafkaAuthentication brokerAuthentication;
    protected final AbstractBrokerPlugin brokerPlugin;
    protected final CountDownLatch latch;
    protected final KafkaCommon kafkaCommon;
    protected BrokerExceptionHandler exceptionHandler;
    protected String consumerGroupName;

    public ApacheKafkaMessageConsumer(ObjectProvider<KafkaConsumer<byte[], byte[]>> kafkaConsumerObjectProvider, List<String> topicList,
                                      ApacheKafkaIdentity brokerIdentity, ApacheKafkaAuthentication brokerAuthentication,
                                      AbstractBrokerPlugin brokerPlugin, CountDownLatch latch, BrokerExceptionHandler exceptionHandler,
                                      KafkaCommon kafkaCommon) {
        super();
        this.kafkaConsumerObjectProvider = kafkaConsumerObjectProvider;
        this.topicList = topicList;
        this.brokerIdentity = brokerIdentity;
        this.brokerAuthentication = brokerAuthentication;
        this.brokerPlugin = brokerPlugin;
        this.latch = latch;
        this.exceptionHandler = exceptionHandler;
        this.kafkaCommon = kafkaCommon;
    }

    public abstract void start();

    public abstract void stop();

    protected Properties getProperties(final Properties... additionalProperties) {
        consumerGroupName = "EventListeningConsumer" + UUID.randomUUID();
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerIdentity.getHost());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupName);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        kafkaCommon.setKafkaClientProperties(properties, false);
        return mergeProperties(properties, additionalProperties);
    }

    private static Properties mergeProperties(final Properties properties, final Properties... additionalProperties) {
        final Properties mergedProperties = new Properties();
        mergedProperties.putAll(properties);
        Stream.of(additionalProperties).forEach(mergedProperties::putAll);
        return mergedProperties;
    }

    public RawKafkaRecord deserialize(ConsumerRecord<byte[], byte[]> record) {
        final byte[] key = record.key();
        final byte[] value = record.value();

        // TODO, what to do if this is an avro record?
        // One option is to remove the first 5 bytes (magic number + schema id) and
        // process as normal after that

        return RawKafkaRecord.builder()
                .key(key)
                .value(value)
                .topic(record.topic())
                .receivedTimestamp(System.currentTimeMillis())
                .createdTimestamp(record.timestamp())
                .build();
    }


}
