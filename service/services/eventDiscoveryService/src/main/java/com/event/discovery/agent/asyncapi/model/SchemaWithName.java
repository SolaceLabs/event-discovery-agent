package com.event.discovery.agent.asyncapi.model;

import lombok.Data;

@Data
public class SchemaWithName {
    private String type;
    private String title;
    private String name;
    private String namespace;
}
