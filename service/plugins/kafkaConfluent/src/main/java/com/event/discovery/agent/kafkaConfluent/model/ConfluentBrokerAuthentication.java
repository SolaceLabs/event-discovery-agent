package com.event.discovery.agent.kafkaConfluent.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.event.discovery.agent.framework.model.ServiceAuthentication;
import com.event.discovery.agent.framework.model.broker.BrokerAuthentication;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaAuthentication;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonDeserialize(as = ConfluentBrokerAuthentication.class)
public class ConfluentBrokerAuthentication extends ApacheKafkaAuthentication implements BrokerAuthentication {

    private ServiceAuthentication schemaRegistry;
}
