package com.event.discovery.agent.task.realtime;

import com.event.discovery.agent.apps.eventDiscovery.EventDiscoveryContainer;
import com.event.discovery.agent.task.TaskRunnerJob;
import com.event.discovery.agent.framework.exception.BrokerException;
import com.event.discovery.agent.framework.exception.DiscoverySupportCode;
import com.event.discovery.agent.framework.exception.EventDiscoveryAgentException;
import com.event.discovery.agent.framework.model.AppStatus;
import com.event.discovery.agent.framework.model.JobStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BrokerConsumer extends RealtimeBase implements TaskRunnerJob {
    public BrokerConsumer(final EventDiscoveryContainer eventDiscoveryContainer) {
        super(eventDiscoveryContainer);
    }

    @Override
    public void start() {
        try {
            getBrokerPlugin().startSubscribers((e) -> {
                String errorMsg;
                if (e instanceof BrokerException) {
                    errorMsg = e.getMessage();
                    log.error(errorMsg, e);
                } else {
                    errorMsg = "Failed while consuming messages: " + e.getMessage();
                    log.error(errorMsg, e);
                }
                getJob().setStatus(JobStatus.ERROR);
                getJob().setError(errorMsg);
                getEventDiscoveryApplication().updateAppStatus(AppStatus.ERROR);
                throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101, e.getMessage());
            });
        } catch (final Exception e) {
            String errorMsg = "Failed to start broker consumer: " + e.getMessage();
            log.error(errorMsg, e);
            getJob().setStatus(JobStatus.ERROR);
            getJob().setError(errorMsg);
            getEventDiscoveryApplication().updateAppStatus(AppStatus.ERROR);
            throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101, e.getMessage());
        }
    }

    @Override
    public void stop() {
        try {
            getBrokerPlugin().stopSubscribers();
        } catch (final Exception e) {
            log.error("Error when stopping broker consumer: " + e.getMessage());
        } finally {
            getBrokerPlugin().closeSession();
        }
    }

    @Override
    public void cancel() {
        stop();
    }
}

