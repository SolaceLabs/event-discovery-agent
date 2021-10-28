package com.event.discovery.agent.solace.model;

import java.util.List;

public interface Endpoint {
    boolean isDurable();

    boolean isExclusive();

    List<Subscription> getSubscriptions();
}
