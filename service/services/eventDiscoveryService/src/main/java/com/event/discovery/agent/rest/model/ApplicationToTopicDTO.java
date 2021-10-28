package com.event.discovery.agent.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ApplicationToTopicDTO {
    private String clientType;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> consumesOnChannels = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> producesOnChannels = new ArrayList<>();
}
