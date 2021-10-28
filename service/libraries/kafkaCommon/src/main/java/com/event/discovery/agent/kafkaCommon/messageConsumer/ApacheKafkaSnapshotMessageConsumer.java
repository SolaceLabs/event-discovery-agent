package com.event.discovery.agent.kafkaCommon.messageConsumer;

import com.event.discovery.agent.framework.AbstractBrokerPlugin;
import com.event.discovery.agent.framework.exception.BrokerException;
import com.event.discovery.agent.framework.exception.BrokerExceptionHandler;
import com.event.discovery.agent.kafkaCommon.KafkaCommon;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaAuthentication;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaIdentity;
import com.event.discovery.agent.kafkaCommon.model.RawKafkaRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.admin.DeleteConsumerGroupsResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

@Slf4j
public class ApacheKafkaSnapshotMessageConsumer extends ApacheKafkaMessageConsumer {
    // the interval and max poll records will help to ensure we never hit a timeout
    // these likely may want to be configurable or tinkered with in the future
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(5);
    private static final int MAX_POLL_RECORDS = 5;

    public ApacheKafkaSnapshotMessageConsumer(ObjectProvider<KafkaConsumer<byte[], byte[]>> kafkaConsumerObjectProvider, List<String> topicList,
                                              ApacheKafkaIdentity brokerIdentity, ApacheKafkaAuthentication brokerAuthentication,
                                              AbstractBrokerPlugin brokerPlugin, CountDownLatch latch, BrokerExceptionHandler exceptionHandler,
                                              KafkaCommon kafkaCommon) {
        super(kafkaConsumerObjectProvider, topicList, brokerIdentity, brokerAuthentication, brokerPlugin, latch, exceptionHandler,
                kafkaCommon);
    }

    @Override
    public void start() {
        try {
            consumeRecords();
        } catch (Exception e) {
            exceptionHandler.handleException(new BrokerException("Unable to consume records", e));
        }
    }

    @Override
    public void stop() {
        if (StringUtils.isNotEmpty(consumerGroupName)) {
            DeleteConsumerGroupsResult deleteConsumerGroupsResult = kafkaCommon.getAdminClient().deleteConsumerGroups(List.of(consumerGroupName));
            try {
                deleteConsumerGroupsResult.all().get();
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Could not delete consumer group", e);
            }
        }
    }

    private void consumeRecords() {
        Properties additionalProperties = new Properties();
        additionalProperties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, MAX_POLL_RECORDS);

        try (KafkaConsumer<byte[], byte[]> consumer = kafkaConsumerObjectProvider.getObject(getProperties(additionalProperties))) {
            consumer.subscribe(topicList);

            // need to do this to connect and get partitions, we will discard these results
            consumer.poll(POLL_INTERVAL);

            // This will iterate over all of the topic partitions and build a map of topic -> partition/offset
            // for the latest message for each topic
            Map<String, Pair<TopicPartition, Long>> topicToLastMessage = new HashMap<>();
            consumer.endOffsets(consumer.assignment()).forEach((topicPartition, offset) -> {
                Optional<Pair<TopicPartition, Long>> topicCandidate = Optional.ofNullable(topicToLastMessage.get(topicPartition.topic()));
                topicCandidate.ifPresentOrElse(candidate -> {
                    if (offset > candidate.getRight()) {
                        topicToLastMessage.put(topicPartition.topic(), Pair.of(topicPartition, offset));
                    }
                }, () -> topicToLastMessage.put(topicPartition.topic(), Pair.of(topicPartition, offset)));
            });

            topicToLastMessage.values().forEach(pair -> {
                TopicPartition topicPartition = pair.getLeft();
                Long offset = pair.getRight();

                // go to the last message
                consumer.seek(topicPartition, (offset == 0) ? offset : offset - 1);

                final ConsumerRecords<byte[], byte[]> records = consumer.poll(POLL_INTERVAL);
                if (records.isEmpty() && offset > 0) {
                    log.warn("No messages were found for topic '" + topicPartition.topic()
                            + "'. It's possible a timeout occurred.");
                }
                records.forEach(record -> {
                    RawKafkaRecord rawKafkaRecord = deserialize(record);
                    brokerPlugin.queueReceivedMessage(rawKafkaRecord);
                });
            });
        } catch (final Exception e) {
            exceptionHandler.handleException(e);
        } finally {
            latch.countDown();
        }
    }
}
