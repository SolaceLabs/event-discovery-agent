package com.event.discovery.agent.solace.model;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class Client {
    private String name;
    private Set<Subscription> rxSubscriptions;
    private Set<Destination> txDestination; // queue / topics that this client is publishing to
    private Set<Endpoint> rxEndpoints; // queue / DTE list that this client is bound to
    private ClientUsername clientUsername; // Reverse link for convenience
    private ClientProfile clientProfile;
    private AclProfile aclProfile;
}
