package com.event.discovery.agent.kafkaCommon.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class KafkaConnector {
    private String name;
    private Map<String, Object> config;
    private List<KafkaConnectorTask> tasks;
    private ConnectorType type;

    public enum ConnectorType {
        SOURCE("source"),
        SINK("sink");
        private final static Map<String, KafkaConnector.ConnectorType> connectorMap =
                Arrays.stream(ConnectorType.values())
                        .collect(Collectors.toMap(ConnectorType::name, Function.identity()));

        private final String connectorType;

        ConnectorType(String connectorType) {
            this.connectorType = connectorType;
        }

        @JsonValue
        String getConnectorType() {
            return connectorType;
        }

        public static KafkaConnector.ConnectorType fromValue(String value) {
            KafkaConnector.ConnectorType constant = connectorMap.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }
    }
}
