package com.event.discovery.agent.apps.model;

import com.event.discovery.agent.apps.model.v1.DiscoveryOperationRequestBO;
import com.event.discovery.agent.framework.model.AppStatus;
import com.event.discovery.agent.framework.model.Job;

public interface DiscoveryApp {
    void start(DiscoveryOperationRequestBO operation, Job job);

    void stop(AppStopReason stopReason);

    EventDiscoveryOperationResultBO getResults();

    AppStatus getStatus();

    void setError(String errorMessage);

    String getError();
}
