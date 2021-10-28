package com.event.discovery.agent.kafkaCommon.model.messages;


import com.event.discovery.agent.framework.model.NormalizedEvent;
import com.event.discovery.agent.framework.model.SchemaType;

public interface KafkaNormalizedEvent extends NormalizedEvent {
    String getKeySchema();

    void setKeySchema(String keySchema);

    SchemaType getKeySchemaType();

    void setKeySchemaType(SchemaType schemaType);

    String getKeySchemaVersion();

    void setKeySchemaId(String schemaId);

    String getKeySchemaId();

    void setKeySubjectName(String subjectName);

    String getKeySubjectName();
}
