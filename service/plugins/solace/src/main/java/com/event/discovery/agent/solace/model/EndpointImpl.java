package com.event.discovery.agent.solace.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder(builderMethodName = "endpointBuilder")
@EqualsAndHashCode(callSuper = true)
public class EndpointImpl extends Channel implements Endpoint {
    private boolean durable;
    private boolean exclusive;
    private List<Subscription> subscriptions;
}
