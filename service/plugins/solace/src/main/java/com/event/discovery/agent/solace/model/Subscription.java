package com.event.discovery.agent.solace.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
public class Subscription {
    private String rawSubscription;
    private String subscription;
    private String sharedName;
    private boolean exported;
}
