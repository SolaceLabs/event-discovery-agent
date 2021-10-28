package com.event.discovery.agent.framework.model;

import com.event.discovery.agent.framework.VendorBrokerPlugin;
import com.event.discovery.agent.framework.model.broker.BrokerAuthentication;
import com.event.discovery.agent.framework.model.broker.BrokerCapabilities;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
public class BasicPluginConfigData implements PluginConfigData {
    private BrokerCapabilities brokerCapabilities;
    private String pluginName;
    private Map<String, Object> inputSchema;
    private Class<? extends BrokerAuthentication> brokerAuthenticationClass;
    private Class<? extends BrokerIdentity> brokerIdentityClass;
    private String brokerType;
    private Class<? extends VendorBrokerPlugin> pluginClass;
}
