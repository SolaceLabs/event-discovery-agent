package com.event.discovery.agent.solace.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClientUsername {
    private String name;
    private List<Client> clients;
    private ClientProfile clientProfile;
    private AclProfile aclProfile;
}
