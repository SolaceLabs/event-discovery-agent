package com.event.discovery.agent.framework.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder(builderMethodName = "noArgsBuilder")
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveryOperation {
    private String operationType;
    private int messageQueueLength;
    private int durationInSecs;
    private Set<String> subscriptionSet;
    private String name;
}
