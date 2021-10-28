package com.event.discovery.agent.framework.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ServiceIdentity {
    private String hostname;
    private int port;
}
