package com.event.discovery.agent.framework;

import com.event.discovery.agent.framework.exception.BrokerExceptionHandler;
import com.event.discovery.agent.framework.model.BrokerConfigData;
import com.event.discovery.agent.framework.model.NormalizedEvent;
import com.event.discovery.agent.framework.model.PluginConfigData;
import com.event.discovery.agent.framework.model.broker.BrokerCapabilities;

public interface VendorBrokerPlugin extends BrokerPlugin {

    PluginConfigData getPluginConfiguration();

    BrokerConfigData getBrokerConfig();

    void startSubscribers(BrokerExceptionHandler brokerExceptionHandler);

    NormalizedEvent normalizeMessage(Object vendorSpecificMessage);

    void stopSubscribers();

    void closeSession();

    boolean hasCapability(BrokerCapabilities.Capability capability);
}
