package com.event.discovery.agent.framework.model.broker;

import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class BrokerCapabilities {
    private Set<Capability> capabilities = new HashSet<>();

    public enum Capability {
        SCHEMA_REGISTRY,            // Plugin uses a schema registry
        TIMED_MESSAGE_CONSUMER,     // Plugin creates a consumer and collects events for a set period of time
        NO_SCHEMA_INFERENCE
    }

    public boolean supports(Capability capability) {
        return capabilities.contains(capability);
    }

    public void add(List<Capability> capabilities) {
        capabilities.addAll(capabilities);
    }
}
