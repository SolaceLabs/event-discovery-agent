package com.event.discovery.agent.rest.model.v1;


import com.event.discovery.agent.framework.model.DiscoveryOperation;
import com.event.discovery.agent.framework.model.broker.BrokerAuthentication;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;
import lombok.Data;

@Data
public class DiscoveryOperationResponseDTO {
    private BrokerIdentity brokerIdentity;
    private BrokerAuthentication brokerAuthentication;
    private DiscoveryOperation discoveryOperation;
    private String jobId;
}
