package com.event.discovery.agent.apps.eventDiscovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.event.discovery.agent.framework.model.NormalizedEvent;
import com.event.discovery.agent.framework.model.NormalizedMsg;
import com.event.discovery.agent.framework.model.SchemaType;
import com.event.discovery.agent.framework.utils.IDGenerator;
import com.event.discovery.agent.framework.utils.JsonSchemaGenerator;
import com.event.discovery.agent.kafkaCommon.model.messages.KafkaNormalizedEvent;
import com.event.discovery.agent.kafkaCommon.model.messages.KafkaNormalizedMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.event.discovery.agent.framework.utils.PayloadTools.getSchemaType;

@Slf4j
public class NormalizedMessageToEventMapper {
    public final EventDiscoveryApp eventDiscoveryApp;
    public final JsonSchemaGenerator jsonSchemaGenerator;
    public final IDGenerator idGenerator;
    public final ObjectMapper objectMapper;

    public NormalizedMessageToEventMapper(EventDiscoveryApp eventDiscoveryApp, JsonSchemaGenerator jsonSchemaGenerator,
                                          IDGenerator idGenerator, ObjectMapper objectMapper) {
        this.eventDiscoveryApp = eventDiscoveryApp;
        this.jsonSchemaGenerator = jsonSchemaGenerator;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    public void mapNormalizedMessageToNormalizedEvent(NormalizedEvent normalizedEvent) {
        // Check if the message has already been processed.
        if (!eventDiscoveryApp.eventInList(normalizedEvent)) {

            if (normalizedEvent.isFromSchemaRegistry()) {
                // This schema is from a schema registry, so no need to do
                // schema inference
                if (!(StringUtils.isEmpty(normalizedEvent.getSchema()))) {
                    // If we already have a schema, move it to the processed event list
                    eventDiscoveryApp.addEventToList(normalizedEvent);
                } else {
                    // No schema for this event?
                    if (!(StringUtils.isEmpty(normalizedEvent.getChannelName()))) {
                        log.warn("Received event without schema on topic {}", normalizedEvent.getChannelName());
                    } else {
                        log.warn("Received event without schema or topic");
                    }
                }
            } else {
                if (!eventDiscoveryApp.noSchemaInference()) {
                    mapMessageToEventWithSchema(normalizedEvent);
                } else {
                    // We are just using the topic, so no need to do any processing
                    eventDiscoveryApp.addEventToList(normalizedEvent);
                }
            }
        } else {
            log.trace("Duplicate event");
        }
    }

    private void mapMessageToEventWithSchema(NormalizedEvent normalizedEvent) {
        NormalizedMsg normalizedMessage = normalizedEvent.getNormalizedMessage();
        SchemaType schemaType = getSchemaTypeFromMessage(normalizedEvent);
        switch (schemaType) {
            case JSONSCHEMA:
                String schema = null;
                try {
                    schema = jsonSchemaGenerator.outputAsString(normalizedEvent.getChannelName(),
                            new String(normalizedMessage.getPayload(), StandardCharsets.UTF_8));
                } catch (IOException | JsonSyntaxException e) {
                    log.error("Could not generate schema from payload", e);
                }
                if (schema != null) {
                    normalizedEvent.setSchema(schema);
                    normalizedEvent.setSchemaType(SchemaType.JSONSCHEMA);
                    normalizedEvent.setSchemaId(normalizedEvent.getChannelName());
                    eventDiscoveryApp.addEventToList(normalizedEvent);
                }
                processMessageKey(normalizedEvent);
                break;
            case TEXT:
                normalizedEvent.setSchemaType(SchemaType.TEXT);
                normalizedEvent.setSchemaId(normalizedEvent.getChannelName());
                normalizedEvent.setSchema("TEXT");
                eventDiscoveryApp.addEventToList(normalizedEvent);
                processMessageKey(normalizedEvent);
                break;
            case UNKNOWN:
            case AVRO:
            default:
                log.warn("Unhandled schema type {}", schemaType);
                break;
        }
    }

    private void processMessageKey(NormalizedEvent normalizedEvent) {
        if (normalizedEvent instanceof KafkaNormalizedEvent) {
            KafkaNormalizedMessage kafkaNormalizedMessage = (KafkaNormalizedMessage) normalizedEvent.getNormalizedMessage();
            KafkaNormalizedEvent kafkaNormalizedEvent = (KafkaNormalizedEvent) normalizedEvent;
            if (kafkaNormalizedMessage.getKey() != null && kafkaNormalizedMessage.getKey().length > 0) {
                SchemaType keySchemaType = getSchemaType(kafkaNormalizedMessage.getKey());
                kafkaNormalizedEvent.setKeySchemaType(keySchemaType);
                switch (keySchemaType) {
                    case JSONSCHEMA:
                        String keySchema = null;
                        try {
                            keySchema = jsonSchemaGenerator.outputAsString(normalizedEvent.getChannelName(),
                                    new String(kafkaNormalizedMessage.getKey(), StandardCharsets.UTF_8));
                        } catch (IOException | JsonSyntaxException e) {
                            log.error("Could not generate key schema from payload", e);
                        }
                        if (keySchema != null) {
                            kafkaNormalizedEvent.setKeySchema(keySchema);
                        }
                        break;
                    case TEXT:
                        kafkaNormalizedEvent.setKeySchema("TEXT");
                        break;
                    case UNKNOWN:
                    case AVRO:
                    default:
                        log.warn("Unhandled key schema type {}", keySchemaType);
                        break;
                }
                kafkaNormalizedEvent.setKeySchemaId(getKeySchemaId(kafkaNormalizedEvent));
            }
        }
    }

    protected SchemaType getSchemaTypeFromMessage(NormalizedEvent normalizedEvent) {

        // If the plugin already told us the schema type
        // then use it
        if (normalizedEvent.getSchemaType() != null) {
            return normalizedEvent.getSchemaType();
        }

        NormalizedMsg normalizedMessage = normalizedEvent.getNormalizedMessage();

        // We need to determine the schema type
        byte[] payload = normalizedMessage.getPayload();
        return getSchemaType(payload);
    }

    private String getKeySchemaId(KafkaNormalizedEvent event) {
        if (keyExists(event)) {
            if (event.getKeySchemaType() == SchemaType.AVRO) {
                return eventDiscoveryApp.getSchemaNameFromAvroSchemaAndVersion(event.getKeySchema(),
                        event.getKeySchemaVersion(), event.getChannelName(), true);
            }
            return event.getChannelName() + "-key";
        }
        return null;
    }

    public static boolean keyExists(KafkaNormalizedEvent event) {
        return event.getKeySchemaType() != null;
    }
}
