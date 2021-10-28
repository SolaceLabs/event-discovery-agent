package io.nats.agent.plugin.model;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class StreamConfig {
    private String name;
    private Set<String> subjects = new HashSet<>();
}
