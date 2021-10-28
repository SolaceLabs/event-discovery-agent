package com.event.discovery.agent.kafkaCommon.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.event.discovery.agent.framework.model.ServiceIdentity;
import com.event.discovery.agent.framework.model.broker.BasicBrokerIdentity;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonDeserialize(as = ApacheKafkaIdentity.class)
public class ApacheKafkaIdentity extends BasicBrokerIdentity implements BrokerIdentity {
    private String host;
    private ServiceIdentity connector;
}
