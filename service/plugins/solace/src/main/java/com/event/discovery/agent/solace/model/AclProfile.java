package com.event.discovery.agent.solace.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AclProfile {
    private String name;
}
