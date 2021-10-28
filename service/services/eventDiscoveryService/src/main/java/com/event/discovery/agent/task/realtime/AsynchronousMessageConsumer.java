package com.event.discovery.agent.task.realtime;

import com.event.discovery.agent.apps.eventDiscovery.EventDiscoveryContainer;
import com.event.discovery.agent.framework.exception.DiscoverySupportCode;
import com.event.discovery.agent.framework.exception.EventDiscoveryAgentException;
import com.event.discovery.agent.framework.model.AppStatus;
import com.event.discovery.agent.framework.model.JobStatus;
import com.event.discovery.agent.framework.model.NormalizedEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class AsynchronousMessageConsumer extends RealtimeMessage implements Runnable {
    public AsynchronousMessageConsumer(final EventDiscoveryContainer eventDiscoveryContainer,
                                       final LinkedBlockingQueue<NormalizedEvent> normalizedMessageProcessingQueue) {
        super(eventDiscoveryContainer, normalizedMessageProcessingQueue);
    }

    @Override
    public void run() {
        try {
            poll();
        } catch (final Exception e) {
            String errorMdg = "Failed to consume messages: " + e.getMessage();
            log.error(errorMdg, e);
            getJob().setStatus(JobStatus.ERROR);
            getJob().setError(errorMdg);
            getEventDiscoveryApplication().updateAppStatus(AppStatus.ERROR);
            throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101, e.getMessage());
        }
    }

    protected void poll() {
        Object msg;
        while (!exit) {
            if (getBrokerPlugin().getReceivedMessageQueue().peek() != null) {
                msg = getBrokerPlugin().getReceivedMessageQueue().poll();
                try {
                    NormalizedEvent event = getBrokerPlugin().normalizeMessage(msg);
                    log.trace("Received messages {}", event);
                    if (!normalizedMessageProcessingQueue.offer(event)) {
                        log.debug("Dropping message");
                    }
                } catch (RuntimeException e) {
                    log.error("Could not normalize message", e);
                }
            }
        }
    }
}

