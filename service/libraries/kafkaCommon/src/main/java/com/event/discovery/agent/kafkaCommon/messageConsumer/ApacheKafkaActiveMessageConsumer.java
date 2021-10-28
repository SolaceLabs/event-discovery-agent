package com.event.discovery.agent.kafkaCommon.messageConsumer;

import com.event.discovery.agent.framework.AbstractBrokerPlugin;
import com.event.discovery.agent.framework.exception.BrokerException;
import com.event.discovery.agent.framework.exception.BrokerExceptionHandler;
import com.event.discovery.agent.kafkaCommon.KafkaCommon;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaAuthentication;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaIdentity;
import com.event.discovery.agent.kafkaCommon.model.RawKafkaRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;


@Slf4j
public class ApacheKafkaActiveMessageConsumer extends ApacheKafkaMessageConsumer {
    private volatile boolean exit = false;
    private final Thread thread;
    private KafkaConsumer<byte[], byte[]> kafkaConsumer;

    public ApacheKafkaActiveMessageConsumer(ObjectProvider<KafkaConsumer<byte[], byte[]>> kafkaConsumerObjectProvider, List<String> topicList,
                                            ApacheKafkaIdentity brokerIdentity, ApacheKafkaAuthentication brokerAuthentication,
                                            AbstractBrokerPlugin brokerPlugin, CountDownLatch latch, BrokerExceptionHandler exceptionHandler,
                                            KafkaCommon kafkaCommon) {
        super(kafkaConsumerObjectProvider, topicList, brokerIdentity, brokerAuthentication, brokerPlugin, latch, exceptionHandler,
                kafkaCommon);
        thread = new Thread() {
            @Override
            public void run() {
                try {
                    consumeRecords();
                } catch (Exception e) {
                    exceptionHandler.handleException(new BrokerException("Unable to consume records", e));
                }
            }
        };
    }

    @Override
    public void start() {
        thread.start();
    }

    private void consumeRecords() {
        try (KafkaConsumer<byte[], byte[]> consumer = kafkaConsumerObjectProvider.getObject(getProperties())) {
            kafkaConsumer = consumer;
            kafkaConsumer.subscribe(topicList);

            // The plugin thread is waiting for us to subscribe. Now that we are subscribed, release
            // the latch
            latch.countDown();

            while (!exit) {
                final ConsumerRecords<byte[], byte[]> records = kafkaConsumer.poll(Duration.ofMillis(100));
                for (final ConsumerRecord<byte[], byte[]> record : records) {
                    RawKafkaRecord rawKafkaRecord = deserialize(record);
                    brokerPlugin.queueReceivedMessage(rawKafkaRecord);
                }
            }
        }
    }

    @Override
    public void stop() {
        exit = true;
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
    }
}
