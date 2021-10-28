package com.event.discovery.agent.framework.model;

import com.event.discovery.agent.framework.VendorBrokerPlugin;
import com.event.discovery.agent.framework.model.broker.BrokerAuthentication;
import com.event.discovery.agent.framework.model.broker.BrokerCapabilities;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;

import java.util.Map;

public interface PluginConfigData {
    BrokerCapabilities getBrokerCapabilities();

    String getPluginName();

    Map<String, Object> getInputSchema();

    Class<? extends BrokerIdentity> getBrokerIdentityClass();

    Class<? extends BrokerAuthentication> getBrokerAuthenticationClass();

    String getBrokerType();

    Class<? extends VendorBrokerPlugin> getPluginClass();
}
