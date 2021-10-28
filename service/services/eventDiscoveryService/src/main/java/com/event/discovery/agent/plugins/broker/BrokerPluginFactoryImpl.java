package com.event.discovery.agent.plugins.broker;

import com.event.discovery.agent.plugins.PluginConfigurationMap;
import com.event.discovery.agent.framework.BrokerPluginFactory;
import com.event.discovery.agent.framework.VendorBrokerPlugin;
import com.event.discovery.agent.framework.exception.DiscoverySupportCode;
import com.event.discovery.agent.framework.exception.EventDiscoveryAgentException;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class BrokerPluginFactoryImpl implements BrokerPluginFactory {

    private Map<String, Class<? extends VendorBrokerPlugin>> brokerTypeToPluginMap = new HashMap<>();
    private final ApplicationContext applicationContext;

    @Autowired
    public BrokerPluginFactoryImpl(ApplicationContext applicationContext,
                                   PluginConfigurationMap pluginConfigurationMap) {
        this.applicationContext = applicationContext;
        pluginConfigurationMap.getPluginConfigurationMap()
                .forEach((k, v) -> {
                    brokerTypeToPluginMap.put(k, v.getPluginClass());
                });
    }

    @Override
    public VendorBrokerPlugin getBrokerPlugin(BrokerIdentity brokerIdentity) {
        String brokerType = brokerIdentity.getBrokerType();
        if (brokerTypeToPluginMap.keySet().contains(brokerType)) {
            ObjectProvider pluginProvider = applicationContext.getBeanProvider(brokerTypeToPluginMap.get(brokerType));
            return (VendorBrokerPlugin) pluginProvider.getObject();
        }
        throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101,
                "Unsupported plugin vendor " + brokerType);
    }
}
