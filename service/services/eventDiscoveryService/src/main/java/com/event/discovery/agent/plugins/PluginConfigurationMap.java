package com.event.discovery.agent.plugins;

import com.event.discovery.agent.framework.VendorBrokerPlugin;
import com.event.discovery.agent.framework.model.PluginConfigData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PluginConfigurationMap {
    private final Map<String, PluginConfigData> pluginConfigurationMap;

    @Autowired
    public PluginConfigurationMap(List<VendorBrokerPlugin> vendorBrokerPluginList) {
        pluginConfigurationMap = vendorBrokerPluginList.stream()
                .map(plugin -> plugin.getPluginConfiguration())
                .collect(Collectors.toMap(PluginConfigData::getBrokerType, Function.identity()));
    }

    public Map<String, PluginConfigData> getPluginConfigurationMap() {
        return pluginConfigurationMap;
    }
}
