package com.event.discovery.agent.solace.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode
public class Channel {
    private String name;
    private ChannelType type;
}
