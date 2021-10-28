package com.event.discovery.agent.apps.eventDiscovery;

import com.event.discovery.agent.apps.model.DiscoveryApp;
import com.event.discovery.agent.framework.model.Job;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EventDiscoveryContainer {
    private Job job;
    private DiscoveryApp eventDiscoveryApplication;
    private Object operationRequest;
}
