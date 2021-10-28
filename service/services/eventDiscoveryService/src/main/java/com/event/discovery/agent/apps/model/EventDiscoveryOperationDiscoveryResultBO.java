package com.event.discovery.agent.apps.model;

import com.event.discovery.agent.framework.model.EventDiscoveryObjects;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class EventDiscoveryOperationDiscoveryResultBO extends EventDiscoveryOperationResultBO {
    private Map<String, Object> configuration;
    private EventDiscoveryObjects events;
    private String discoverySchemaVersion;
    private long discoveryStartTime;
    private long discoveryEndTime;
}
