package com.event.discovery.agent.solace.model;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;


@SuperBuilder(builderMethodName = "topicEndpointBuilder")
@EqualsAndHashCode(callSuper = true)
public class TopicEndpoint extends EndpointImpl implements Endpoint {
}
