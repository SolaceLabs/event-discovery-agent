package com.event.discovery.agent.task.realtime;

import com.event.discovery.agent.apps.eventDiscovery.EventDiscoveryContainer;
import com.event.discovery.agent.framework.model.NormalizedEvent;

import java.util.concurrent.LinkedBlockingQueue;

public abstract class RealtimeMessage extends RealtimeBase {
    protected boolean exit = false;
    protected LinkedBlockingQueue<NormalizedEvent> normalizedMessageProcessingQueue;

    public RealtimeMessage(final EventDiscoveryContainer eventDiscoveryContainer,
                           final LinkedBlockingQueue<NormalizedEvent> normalizedMessageProcessingQueue) {
        super(eventDiscoveryContainer);
        this.normalizedMessageProcessingQueue = normalizedMessageProcessingQueue;
    }

    public void stop() {
        exit = true;
    }

    public void cancel() {
        exit = true;
    }
}
