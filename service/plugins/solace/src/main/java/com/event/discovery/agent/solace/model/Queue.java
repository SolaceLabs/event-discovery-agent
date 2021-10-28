package com.event.discovery.agent.solace.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Queue extends EndpointImpl implements Destination, Endpoint {
}
