package com.event.discovery.agent.framework.model;

public interface DiscoveryAppSb {
    void updateAppStatus(AppStatus appStatus);

    void setError(String error);
}
