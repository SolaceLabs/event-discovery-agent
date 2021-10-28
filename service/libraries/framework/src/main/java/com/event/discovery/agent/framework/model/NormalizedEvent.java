package com.event.discovery.agent.framework.model;

import java.util.List;
import java.util.Map;

public interface NormalizedEvent {

    String getChannelName();

    String getChannelType();

    // Include the schema if its available
    String getSchema();

    void setSchema(String schema);

    SchemaType getSchemaType();

    String getVersion();

    void setSchemaType(SchemaType schemaType);

    // Hints for the application stitching
    List<MessagingClient> getReceiverIds();

    List<MessagingClient> getSenderIds();

    // For additional hints for application stitching
    Map<String, Object> getAdditionalParameters();

    // Get the message that represents this event
    NormalizedMsg getNormalizedMessage();

    String getSchemaId();

    void setSchemaId(String eventId);

    boolean isFromSchemaRegistry();

    void setSubjectName(String subjectName);

    String getSubjectName();
}
