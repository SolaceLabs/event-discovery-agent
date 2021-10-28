package com.event.discovery.agent.solace.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder(builderMethodName = "provEndpointBuilder")
@EqualsAndHashCode(callSuper = true)
public class ProvisionedEndpoint extends Channel {
    private boolean durable;
    private boolean exclusive;
    private List<Subscription> subscriptions;
}
