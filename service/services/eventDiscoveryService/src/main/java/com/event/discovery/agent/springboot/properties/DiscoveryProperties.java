package com.event.discovery.agent.springboot.properties;

import com.event.discovery.agent.framework.utils.SemanticVersion;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "event.discovery")
@Getter
@Setter
@NoArgsConstructor
public class DiscoveryProperties {

    private String id;
    private boolean offline;
    private SemanticVersion version;
    private JobProperties jobProperties;
    private int numConsumers;
    private String autoUploadHost;
    private boolean includeConfiguration;
}
