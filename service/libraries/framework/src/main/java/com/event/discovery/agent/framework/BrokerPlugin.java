package com.event.discovery.agent.framework;

import com.event.discovery.agent.framework.model.DiscoveryOperation;
import com.event.discovery.agent.framework.model.broker.BrokerAuthentication;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;

import java.util.concurrent.LinkedBlockingQueue;

public interface BrokerPlugin {
    void initialize(BrokerIdentity brokerIdentity, BrokerAuthentication brokerAuthentication,
                    DiscoveryOperation discoveryOperation);

    LinkedBlockingQueue<?> getReceivedMessageQueue();
}
