package com.event.discovery.agent.kafkaCommon.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "plugin.kafka.common")
public class KafkaCommonProperties {
    private List<String> kafkaConnectorTopicFieldList;
}
