package com.event.discovery.agent.apps.eventDiscovery;

import com.event.discovery.agent.apps.model.DiscoveryApp;
import com.event.discovery.agent.framework.model.Job;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class EventDiscoveryService {
    private final Map<String, EventDiscoveryContainer> jobIdToEventDiscoveryMap = new HashMap<>();

    public EventDiscoveryContainer getEventDiscoveryContainer(final String jobId) {
        return jobIdToEventDiscoveryMap.get(jobId);
    }

    public EventDiscoveryContainer getEventDiscoveryContainer(final Job job) {
        return getEventDiscoveryContainer(job.getId());
    }

    public synchronized void createEventDiscoveryContainer(final Job job, final DiscoveryApp discoveryApp, final Object operationRequest) {
        Objects.requireNonNull(job);
        Objects.requireNonNull(discoveryApp);
        Objects.requireNonNull(operationRequest);
        jobIdToEventDiscoveryMap.put(job.getId(), EventDiscoveryContainer.builder()
                .eventDiscoveryApplication(discoveryApp)
                .job(job)
                .operationRequest(operationRequest)
                .build());
    }
}
