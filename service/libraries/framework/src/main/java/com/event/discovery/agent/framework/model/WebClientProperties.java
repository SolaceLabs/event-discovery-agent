package com.event.discovery.agent.framework.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebClientProperties {
    private String trustStoreLocation;
    private String trustStorePassword;
    private String keyStoreLocation;
    private String keyStorePassword;
    private String keyPassword;
}
