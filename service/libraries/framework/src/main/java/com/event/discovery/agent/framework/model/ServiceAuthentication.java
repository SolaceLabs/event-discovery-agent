package com.event.discovery.agent.framework.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ServiceAuthentication {
    private String trustStoreLocation;
    private String trustStorePassword;
    private String keyStoreLocation;
    private String keyStorePassword;
    private String basicAuthUsername;
    private String basicAuthPassword;
}
