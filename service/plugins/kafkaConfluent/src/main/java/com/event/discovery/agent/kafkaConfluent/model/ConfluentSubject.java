package com.event.discovery.agent.kafkaConfluent.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ConfluentSubject {
    private String subject;
    private int version;
    private int id;
    private String schema;
}
