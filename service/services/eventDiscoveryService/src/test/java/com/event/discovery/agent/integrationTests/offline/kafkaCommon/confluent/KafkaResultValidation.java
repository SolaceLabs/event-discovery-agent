package com.event.discovery.agent.integrationTests.offline.kafkaCommon.confluent;

import com.event.discovery.agent.apps.model.EventDiscoveryOperationDiscoveryResultBO;
import com.event.discovery.agent.springboot.properties.DiscoveryProperties;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class KafkaResultValidation {
    private DiscoveryProperties discoveryProperties;

    public KafkaResultValidation(DiscoveryProperties discoveryProperties) {
        this.discoveryProperties = discoveryProperties;
    }

    protected void validateConnector(EventDiscoveryOperationDiscoveryResultBO result) {
        if (discoveryProperties.isIncludeConfiguration()) {
            // Validate the connectors
            List<Map<String, Object>> connectorList = (List<Map<String, Object>>) result.getConfiguration().get("kafka_connect");
            assertEquals(2, connectorList.size());

            Map<String, Object> connector = connectorList.get(0);
            assertEquals("connector1", connector.get("connector_name"));
            assertEquals("SOURCE", connector.get("connector_type"));
            assertEquals("ATopic", connector.get("kafka.topic"));

            List<Map<String, Object>> taskList = (List<Map<String, Object>>) connector.get("connect_tasks");
            Map<String, Object> task = taskList.get(0);
            assertEquals("UNASSIGNED", task.get("task_state"));
            assertEquals(3, task.get("task_id"));
            assertEquals("4", task.get("worker_id"));
        }
    }

    protected void validateTopic(EventDiscoveryOperationDiscoveryResultBO result, int numberOfExpectedTopics) {
        validateTopic(result, numberOfExpectedTopics, "ATopic");
    }

    protected void validateTopic(EventDiscoveryOperationDiscoveryResultBO result, int numberOfExpectedTopics,
                                 String topicName) {
        if (discoveryProperties.isIncludeConfiguration()) {

            // Validate the connectors
            List<Map<String, Object>> topicList = (List<Map<String, Object>>) result.getConfiguration().get("topic");
            assertEquals(numberOfExpectedTopics, topicList.size());

            Map<String, Object> topic = topicList.get(0);
            assertEquals(topicName, topic.get("topic_name"));

            Map<String, Object> topicConfig = (Map<String, Object>) topic.get("topic_config");
            assertEquals((Double) 1D, getDoubleConfigValue((Map<String, Object>) topicConfig.get("retention.ms")));
            assertEquals((Integer) 2, getIntegerConfigValue((Map<String, Object>) topicConfig.get("retention.bytes")));
            assertEquals((Integer) 3, getIntegerConfigValue((Map<String, Object>) topicConfig.get("segment.bytes")));
            assertEquals((Integer) 4, getIntegerConfigValue((Map<String, Object>) topicConfig.get("segment.index.bytes")));
            assertEquals((Double) 5D, getDoubleConfigValue((Map<String, Object>) topicConfig.get("segment.ms")));
            assertEquals((Double) 6D, getDoubleConfigValue((Map<String, Object>) topicConfig.get("segment.jitter.ms")));
            assertEquals((Integer) 7, getIntegerConfigValue((Map<String, Object>) topicConfig.get("min.insync.replicas")));
            assertEquals((Integer) 8, getIntegerConfigValue((Map<String, Object>) topicConfig.get("max.message.bytes")));
            assertEquals((Integer) 9, getIntegerConfigValue((Map<String, Object>) topicConfig.get("index.interval.bytes")));
        }
    }

    private Integer getIntegerConfigValue(Map<String, Object> config) {
        return (Integer) config.get("value");
    }

    private Double getDoubleConfigValue(Map<String, Object> config) {
        return (Double) config.get("value");
    }

}
