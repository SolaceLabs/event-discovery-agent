package com.event.discovery.agent.kafkaConfluent.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.event.discovery.agent.framework.model.ServiceIdentity;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaIdentity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonDeserialize(as = ConfluentKafkaIdentity.class)
public class ConfluentKafkaIdentity extends ApacheKafkaIdentity implements BrokerIdentity {
    private ServiceIdentity schemaRegistry;
}
