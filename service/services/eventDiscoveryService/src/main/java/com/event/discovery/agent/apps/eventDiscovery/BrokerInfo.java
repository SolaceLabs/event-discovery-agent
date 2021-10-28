package com.event.discovery.agent.apps.eventDiscovery;

import com.event.discovery.agent.framework.VendorBrokerPlugin;
import com.event.discovery.agent.framework.model.PluginConfigData;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class BrokerInfo {
    private final VendorBrokerPlugin brokerPlugin;
    private final PluginConfigData pluginConfigData;
    private final Map<String, Object> discoveredConfig;
}
