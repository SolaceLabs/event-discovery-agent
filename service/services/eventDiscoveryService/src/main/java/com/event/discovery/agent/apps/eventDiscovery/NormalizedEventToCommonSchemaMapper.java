package com.event.discovery.agent.apps.eventDiscovery;

import com.event.discovery.agent.types.event.common.Schema;
import com.event.discovery.agent.framework.model.CommonModelConstants;
import com.event.discovery.agent.framework.model.NormalizedEvent;
import com.event.discovery.agent.framework.model.SchemaType;
import com.event.discovery.agent.framework.utils.PayloadTools;
import com.event.discovery.agent.kafkaCommon.model.messages.KafkaNormalizedEvent;
import com.solacesystems.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class NormalizedEventToCommonSchemaMapper {

    private final EventDiscoveryApp discoveryApp;

    public NormalizedEventToCommonSchemaMapper(EventDiscoveryApp discoveryApp) {
        this.discoveryApp = discoveryApp;
    }

    public Map<String, Schema> mapNormalizedEventToSchemas(NormalizedEvent event) {
        Map<String, Schema> schemas = new HashMap<>();
        if (event instanceof KafkaNormalizedEvent) {
            KafkaNormalizedEvent kafkaEvent = (KafkaNormalizedEvent) event;
            Optional.ofNullable(getKeySchema(kafkaEvent))
                    .ifPresent(keySchema ->
                            schemas.put(kafkaEvent.getKeySchemaId(), keySchema)
                    );
        }

        schemas.put(event.getSchemaId(), getValueSchema(event));
        return schemas;
    }


    private String contentTypeMapper(SchemaType schemaType, byte[] content) {
        if (SchemaType.AVRO == schemaType && !PayloadTools.isJson(content)) {
            return "TEXT";
        } else if (SchemaType.AVRO == schemaType && PayloadTools.isJson(content)) {
            return "JSON";
        } else if (SchemaType.JSONSCHEMA == schemaType) {
            return "JSON";
        }
        return schemaType.name();
    }

    private Schema getKeySchema(KafkaNormalizedEvent event) {
        if (hasSchemaKey(event)) {
            return getSchema(
                    event,
                    event.getKeySchemaType(),
                    event.getKeySchema(),
                    event.getKeySchemaVersion(),
                    event.getKeySubjectName(),
                    event.getKeySchemaId());
        }
        return null;
    }

    private Schema getValueSchema(NormalizedEvent event) {
        return getSchema(
                event,
                event.getSchemaType(),
                event.getSchema(),
                event.getVersion(),
                event.getSubjectName(),
                event.getSchemaId());
    }

    private Schema getSchema(
            NormalizedEvent event,
            SchemaType schemaType,
            String content,
            String schemaVersion,
            String subjectName,
            String schemaId) {

        Schema schema = new Schema();
        Map<String, Object> additionalAttrs = new HashMap<>();
        schema.setAdditionalAttributes(additionalAttrs);
        schema.setId(discoveryApp.getNextGeneratedId());
        schema.setSchemaType(schemaType.name());

        String contentType = contentTypeMapper(schemaType, content.getBytes(UTF_8));
        additionalAttrs.put(CommonModelConstants.Schema.CONTENT_TYPE, contentType);
        if ("JSON".equals(contentType)) {
            schema.setContent(content);
            schema.setPrimitive(false);
        } else {
            schema.setPrimitive(true);
            if (SchemaType.AVRO == schemaType) {
                schema.setContent(content);
            }
        }

        if (Boolean.TRUE.equals(event.isFromSchemaRegistry())) {
            additionalAttrs.put(CommonModelConstants.Schema.SCHEMA_REG, true);
            additionalAttrs.put(CommonModelConstants.Schema.SCHEMA_REG_ID, schemaId);
        }

        Optional.ofNullable(subjectName)
                .ifPresent(sn -> additionalAttrs.put(CommonModelConstants.Schema.SUBJECT_NAME, sn));
        Optional.ofNullable(schemaVersion)
                .ifPresent(v -> additionalAttrs.put(CommonModelConstants.Schema.SUBJECT_VERSION, schemaVersion));

        return schema;
    }

    private boolean hasSchemaKey(KafkaNormalizedEvent event) {
        return event.getKeySchemaType() != null && !StringUtil.isEmpty(event.getKeySchema());
    }

}
