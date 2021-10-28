package com.event.discovery.agent.apps.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class EventDiscoveryOperationResultBO {

    private String jobId;
    private String status;

    private String discoveryAgentVersion;
    private String name;
    private String pluginType;
    private Map<String, Object> pluginInputs;
    private String brokerType;
    private List<String> warnings;
    private String error;

}
