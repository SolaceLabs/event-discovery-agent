package com.event.discovery.agent.framework;

import com.event.discovery.agent.framework.model.broker.BrokerIdentity;

public interface BrokerPluginFactory {
    VendorBrokerPlugin getBrokerPlugin(BrokerIdentity brokerIdentity);
}
