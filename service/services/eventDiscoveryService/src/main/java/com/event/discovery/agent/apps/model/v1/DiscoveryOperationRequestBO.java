package com.event.discovery.agent.apps.model.v1;

import com.event.discovery.agent.framework.model.DiscoveryOperation;
import com.event.discovery.agent.framework.model.broker.BrokerAuthentication;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(builderMethodName = "noArgsBuilder")
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveryOperationRequestBO {
    private BrokerIdentity brokerIdentity;
    private BrokerAuthentication brokerAuthentication;
    private DiscoveryOperation discoveryOperation;
}
