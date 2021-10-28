package com.event.discovery.agent.apps.model;

import com.event.discovery.agent.framework.VendorBrokerPlugin;

import java.util.HashMap;
import java.util.Map;

public class PluginCollectionBO {
    private final Map<String, VendorBrokerPlugin> pluginMap = new HashMap<>();

    public void add(Map<String, VendorBrokerPlugin> plugins) {
        pluginMap.putAll(plugins);
    }

    public Map<String, VendorBrokerPlugin> getPluginMap() {
        return pluginMap;
    }

}
