package com.event.discovery.agent.solace.model.broker;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.event.discovery.agent.framework.model.broker.BasicBrokerIdentity;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonDeserialize(as = SolaceBrokerIdentity.class)
public class SolaceBrokerIdentity extends BasicBrokerIdentity implements BrokerIdentity {
    private String sempHost;
    private String clientHost;
    private String clientProtocol;
    private String messagingPort;
    private String messageVpn;
    private String sempPort;
    private String sempScheme;
    private String brokerAlias;
}