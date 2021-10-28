package com.event.discovery.agent.task.realtime;

import com.event.discovery.agent.apps.eventDiscovery.EventDiscoveryApp;
import com.event.discovery.agent.apps.eventDiscovery.EventDiscoveryContainer;
import com.event.discovery.agent.framework.VendorBrokerPlugin;
import com.event.discovery.agent.framework.model.Job;

public abstract class RealtimeBase {
    private final EventDiscoveryContainer eventDiscoveryContainer;

    public RealtimeBase(final EventDiscoveryContainer eventDiscoveryContainer) {
        this.eventDiscoveryContainer = eventDiscoveryContainer;
    }

    protected EventDiscoveryApp getEventDiscoveryApplication() {
        return (EventDiscoveryApp) eventDiscoveryContainer.getEventDiscoveryApplication();
    }

    protected VendorBrokerPlugin getBrokerPlugin() {
        return getEventDiscoveryApplication().getBrokerInfo().getBrokerPlugin();
    }

    protected Job getJob() {
        return eventDiscoveryContainer.getJob();
    }
}
