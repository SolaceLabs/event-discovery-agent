package com.event.discovery.agent.task.realtime;

import com.event.discovery.agent.apps.eventDiscovery.EventDiscoveryContainer;
import com.event.discovery.agent.task.TaskRunnerJob;
import com.event.discovery.agent.framework.SchemaEvents;
import com.event.discovery.agent.framework.exception.DiscoverySupportCode;
import com.event.discovery.agent.framework.exception.EventDiscoveryAgentException;
import com.event.discovery.agent.framework.model.AppStatus;
import com.event.discovery.agent.framework.model.JobStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class BrokerNormalizedEventProcessor extends RealtimeBase implements TaskRunnerJob {
    public BrokerNormalizedEventProcessor(final EventDiscoveryContainer eventDiscoveryContainer) {
        super(eventDiscoveryContainer);
    }

    @Override
    public void start() {
        try {
            processEvents();
        } catch (final Exception e) {
            String errorMsg = "Failed to process events: " + e.getMessage();
            log.error(errorMsg, e);
            getJob().setStatus(JobStatus.ERROR);
            getJob().setError(errorMsg);
            getEventDiscoveryApplication().updateAppStatus(AppStatus.ERROR);
            throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101, e.getMessage());
        }
    }

    protected void processEvents() {
        if (getBrokerPlugin() instanceof SchemaEvents) {
            ((SchemaEvents) getBrokerPlugin()).getNormalizedEvents().stream()
                    .filter(Objects::nonNull)
                    .forEach(event -> getEventDiscoveryApplication().mapNormalizedMessageToNormalizedEvent(event));
        } else {
            log.error("Broker plugin does not figure out its own schemas");
        }
    }
}
