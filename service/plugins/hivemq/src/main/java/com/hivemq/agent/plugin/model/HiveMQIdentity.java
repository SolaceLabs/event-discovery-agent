package com.hivemq.agent.plugin.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.event.discovery.agent.framework.model.broker.BasicBrokerIdentity;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonDeserialize(as = HiveMQIdentity.class)
public class HiveMQIdentity extends BasicBrokerIdentity implements BrokerIdentity {
    private String hostname;
    private String clientProtocol;
    private int clientPort;
    private int adminPort;
}