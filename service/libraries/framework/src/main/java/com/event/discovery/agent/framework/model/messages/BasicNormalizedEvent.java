package com.event.discovery.agent.framework.model.messages;

import com.event.discovery.agent.framework.model.MessagingClient;
import com.event.discovery.agent.framework.model.NormalizedEvent;
import com.event.discovery.agent.framework.model.NormalizedMsg;
import com.event.discovery.agent.framework.model.SchemaType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
public class BasicNormalizedEvent implements NormalizedEvent {
    private String channelName;
    private String channelType;

    // Include the schema if its available
    private String schema;
    private String version;
    private SchemaType schemaType;

    // Hints for the application stitching
    private List<MessagingClient> receiverIds = new ArrayList<>();
    private List<MessagingClient> senderIds = new ArrayList<>();

    // For additional hints for application stitching
    private Map<String, Object> additionalParameters = new HashMap<>();

    private NormalizedMsg normalizedMessage;

    private String schemaId;

    private boolean fromSchemaRegistry;

    private String subjectName;
}
