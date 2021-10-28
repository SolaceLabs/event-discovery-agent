package com.event.discovery.agent.framework.model;

import com.event.discovery.agent.types.event.common.DiscoveryData;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class BrokerConfigData {
    private Map<String, Object> configuration;
    private DiscoveryData commonDiscoveryData;
}
