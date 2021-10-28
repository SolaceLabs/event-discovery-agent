package com.event.discovery.agent.asyncapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.event.discovery.agent.apps.model.EventDiscoveryOperationDiscoveryResultBO;
import com.event.discovery.agent.asyncapi.model.SchemaWithName;
import com.event.discovery.agent.types.event.common.Channel;
import com.event.discovery.agent.types.event.common.DiscoveryData;
import com.event.discovery.agent.types.event.common.Schema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class CommonModelToAsyncAPIMapper_2_2 extends CommonModelToAsyncApiMapper {

    private final String asyncApiTemplate;
    private static final String TEMPLATE_PATH = "classpath:" + File.separator
            + "gen-templates" + File.separator + "asyncapi_2.2.0.json";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    public CommonModelToAsyncAPIMapper_2_2(ResourceLoader resourceLoader) {
        super();
        Resource asyncApiTemplateResource = resourceLoader.getResource(TEMPLATE_PATH);
        asyncApiTemplate = asString(asyncApiTemplateResource);

        // Overwrite the schema format map
        SCHEMA_FORMAT_LOOKUP_MAP = ImmutableMap.<String, String>builder()
                .put("AVRO", "application/vnd.apache.avro;version=1.9.0")
                .put("JSONSCHEMA", "application/vnd.aai.asyncapi+json;version=2.2.0")
                .build();
    }

    // Maps a scan in the agent's internal model into AsyncAPI 2,2
    public JSONObject map(EventDiscoveryOperationDiscoveryResultBO scan) {

        DiscoveryData discovery = (DiscoveryData) scan.getEvents();

        JSONObject root = new JSONObject(asyncApiTemplate);

        addInfoSection(scan, root);

        // Add the schema and message sections under components
        for ( Schema schema : discovery.getSchemas()) {
            String schemaName = getSchemaName(schema);
            addMessage(root, schema, schemaName);
            addSchema(root, schema, schemaName);
        }

        // Build a quick lookup table for topic to schema
        Map<String, List<String>> channelIdToSchemaIdMap = new HashMap<>();
        buildChannelIdToSchemaIdLookupTable(discovery, channelIdToSchemaIdMap);

        // Add each of the channels
        for ( Channel channel: discovery.getChannels()) {
            addChannel(root, channel, channelIdToSchemaIdMap, discovery);
        }

        return root;
    }

    private void addInfoSection(EventDiscoveryOperationDiscoveryResultBO scan, JSONObject root) {
        JSONObject infoNode = root.getJSONObject("info");
        infoNode.put("title", scan.getName())
                .put("description", "Discovery Agent Scan")
                .put("version", scan.getDiscoveryAgentVersion());
    }

    // Builds a table of channelId to the list of schemaIds that it references.
    // Currently ignores kafka keys which are described in the additionalAttributes
    // of the channelToSchemaRelationship.
    private void buildChannelIdToSchemaIdLookupTable(DiscoveryData discovery, Map<String, List<String>> channelIdToSchemaIdMap) {
        discovery.getChannelToSchemaRelationships()
            .forEach(channelIdToSchemaId -> {
                if (!channelIdToSchemaIdMap.containsKey(channelIdToSchemaId.getChannelId())) {
                    channelIdToSchemaIdMap.put(channelIdToSchemaId.getChannelId(), new ArrayList<>());
                }
                channelIdToSchemaIdMap.get(channelIdToSchemaId.getChannelId()).add(channelIdToSchemaId.getSchemaId());
            });
    }

    // Adds a channel and adds a reference to the corresponding #components/message object if
    // one exists. Also inserts "oneOf" if multiple message references exist.
    private void addChannel(JSONObject root, Channel channel, Map<String, List<String>> channelIdToSchemaIdMap,
                            DiscoveryData discovery) {
        if (channel.getAdditionalAttributes() != null && channel.getAdditionalAttributes().containsKey("type")) {
            String channelType = (String) channel.getAdditionalAttributes().get("type");
            if (channelType.equals("topic")) {
                JSONObject channelObj = new JSONObject();
                if (channelIdToSchemaIdMap.containsKey(channel.getId())) {
                    JSONObject messageNode = new JSONObject();
                    channelObj.put("publish", messageNode);

                    if (channelIdToSchemaIdMap.get(channel.getId()).size() == 1) {
                        // Single message
                        String schemaId = channelIdToSchemaIdMap.get(channel.getId()).get(0);
                        JSONObject ref = getMessageRefJSONObject(discovery, schemaId);
                        messageNode.put("message", ref);
                    } else {
                        // Multiple messages
                        JSONArray messageContainer = new JSONArray();
                        messageNode.put("message", new JSONObject().put("oneOf", messageContainer));
                        channelIdToSchemaIdMap.get(channel.getId()).forEach(schemaId -> {
                            JSONObject ref = getMessageRefJSONObject(discovery, schemaId);
                            messageContainer.put(ref);
                        });
                    }
                }
                root.getJSONObject("channels").put(channel.getName(), channelObj);
            }
        }
    }

    // Builds the reference to the message in the #/components/messages section
    private JSONObject getMessageRefJSONObject(DiscoveryData discovery, String schemaId) {
        Optional<Schema> schema = discovery.getSchemas().stream().filter(schem -> schem.getId().equals(schemaId)).findFirst();
        if (schema.isPresent()) {
            String messageRef = "#/components/messages/" + getSchemaName(schema.get());
            return new JSONObject().put("$ref", messageRef);
        }
        return new JSONObject();
    }

    // Adds a message definition in the #/components/messages section corresponding
    // to the discovered schema
    private void addMessage(JSONObject root, Schema schema, String schemaName) {
        root.getJSONObject("components").getJSONObject("messages")
                .put(schemaName, createMessageJsonObjectFromSchema(schema, schemaName));
    }

    // Adds a schema definition in the #/components/schema section based on the
    // schema
    private void addSchema(JSONObject root, Schema schema, String schemaName) {
        if (schema.getPrimitive() || !schema.getSchemaType().toUpperCase(Locale.ROOT).equals("AVRO")
                || !StringUtils.isNotEmpty(schema.getContent())) {
            createJsonObjectFromSchema(schema).ifPresent((schemaObj) ->
                    root.getJSONObject("components").getJSONObject("schemas").put(schemaName, schemaObj));
        }
    }

    // Creates the schema object based on the discovered schema
    private JSONObject createMessageJsonObjectFromSchema(Schema schema, String schemaName) {
        if (ObjectUtils.isEmpty(schema)) {
            return new JSONObject();
        }

        JSONObject message = new JSONObject();
        message.put("name", schemaName);

        if (MEDIA_TYPE_LOOKUP_MAP.containsKey(schema.getSchemaType())) {
            message.put("contentType", MEDIA_TYPE_LOOKUP_MAP.get(schema.getSchemaType()));
        }

        if (SCHEMA_FORMAT_LOOKUP_MAP.containsKey(schema.getSchemaType())) {
            message.put("schemaFormat", SCHEMA_FORMAT_LOOKUP_MAP.get(schema.getSchemaType()));
        }

        JSONObject payload = new JSONObject();
        if (schema.getPrimitive()) {
            String primitiveType = getPrimitiveType(schema);
            payload.put("type", primitiveType);
            message.put("payload", payload);

        } else {
            addNonPrimitiveNonAvroSchemaRef(schema, schemaName, message, payload);
        }
        return message;
    }

    // Adds a reference from the #/component/messages/ section to the corresponding
    // #/components/schema section if the schema is not a primitive type and is
    // not an AVRO schema. AVRO schemas need to be included inline since the
    // AsyncAPI parser doesn't support a schema "record" definition in the
    // #/components/schema section.
    private void addNonPrimitiveNonAvroSchemaRef(Schema schema, String schemaName, JSONObject message, JSONObject payload) {
        if (!schema.getPrimitive() && schema.getSchemaType().toUpperCase(Locale.ROOT).equals("AVRO")
                && StringUtils.isNotEmpty(schema.getContent())) {
            createJsonObjectFromSchema(schema).ifPresent((schemaObj) ->
                    message.put("payload", schemaObj));
        } else {
            payload.put("$ref", "#/components/schemas/" + schemaName);
            message.put("payload", payload);
        }
    }

    // Returns the discovered AVRO primitive type
    private String getPrimitiveType(Schema schema) {
        String primitiveType = "string";

        // For AVRO, the primitive type is encoded in the content
        if (schema.getSchemaType().toUpperCase(Locale.ROOT).equals("AVRO")) {
            primitiveType = schema.getContent().replaceAll("\"", "");
        }
        return primitiveType;
    }

    // Creates a schema object from the discovered schema content
    private Optional<JSONObject> createJsonObjectFromSchema(Schema schema) {
        if (ObjectUtils.isEmpty(schema)) {
            return Optional.empty();
        }

        if (!schema.getPrimitive()) {
            try {
                return Optional.of(new JSONObject(schema.getContent()));
            } catch (Throwable t) {
                // Eat the exception. Not all schemas are in json format
                log.trace(
                        "createJsonObjectFromSchema() - Failed to convert Async Api payload content to a JSON object, content={}, " +
                                "exception={}",
                        schema.getContent().replaceAll("\\s+", ""), t.getMessage());
            }
        }
        return Optional.empty();
    }

    // Try to determine the schema's name based on the AVRO records name and namespace or the
    // JSONSchemas title. Also add a uniqueId to the end of the name since there may be multiple
    // versions of the schema that we need to disambiguate.
    // If we aren't able to determine the schema's name, just use it's id.
    private String getSchemaName(Schema schema) {
        if (!schema.getPrimitive() && StringUtils.isNotEmpty(schema.getContent())) {
            try {
                SchemaWithName schemaWithName = objectMapper.readValue(schema.getContent(), SchemaWithName.class);
                if (StringUtils.isNotEmpty(schemaWithName.getType())) {
                    if (schemaWithName.getType().toUpperCase(Locale.ROOT).equals("RECORD")) {

                        if (StringUtils.isNotEmpty(schemaWithName.getNamespace())) {
                            return schemaWithName.getNamespace() + "." + schemaWithName.getName()
                                    + "-" + schema.getId();
                        } else {
                            // Add the ID as well to guarantee uniqueness
                            return schemaWithName.getName() + "-" + schema.getId();
                        }
                    } else if (schemaWithName.getType().toUpperCase(Locale.ROOT).equals("OBJECT")) {
                        if (StringUtils.isNotEmpty(schemaWithName.getTitle())) {
                            // Add the ID as well to guarantee uniqueness plus
                            // replace '/' with '_' so async can parse references properly
                            return (schemaWithName.getTitle() + "-" + schema.getId()).replace("/", "_");
                        }
                    }
                }
            } catch (JsonProcessingException e) {
                log.warn("Could not serialize");
            }
        }
        return schema.getId();
    }
}
