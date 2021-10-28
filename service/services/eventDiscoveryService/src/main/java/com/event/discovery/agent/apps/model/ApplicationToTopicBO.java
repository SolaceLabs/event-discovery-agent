package com.event.discovery.agent.apps.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@SuperBuilder
public class ApplicationToTopicBO {
    private String clientType;
    private List<String> consumesOnChannels = new ArrayList<>();
    private List<String> producesOnChannels = new ArrayList<>();
}
