package com.event.discovery.agent.kafkaCommon.model.messages;

import com.event.discovery.agent.framework.model.SchemaType;
import com.event.discovery.agent.framework.model.messages.BasicNormalizedEvent;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class KafkaNormalizedEventImpl extends BasicNormalizedEvent implements KafkaNormalizedEvent {
    private String keySchema;
    private SchemaType keySchemaType;
    private String keySchemaVersion;
    private String keySchemaId;
    private String keySubjectName;
}
