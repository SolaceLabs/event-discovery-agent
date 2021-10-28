package com.event.discovery.agent.framework.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class AvroSchema {
    private String type;
    private String name;
    private String namespace;
    private List<Object> fields;
}
