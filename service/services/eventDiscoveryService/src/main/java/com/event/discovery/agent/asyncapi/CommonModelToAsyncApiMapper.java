package com.event.discovery.agent.asyncapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.event.discovery.agent.apps.model.EventDiscoveryOperationDiscoveryResultBO;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public abstract class CommonModelToAsyncApiMapper {
    public abstract JSONObject map(EventDiscoveryOperationDiscoveryResultBO scan);

    protected final static Map<String, String> MEDIA_TYPE_LOOKUP_MAP = ImmutableMap.<String, String>builder()
            .put("AVRO", "application/json")
            .put("BINARY", "application/octet-stream")
            .put("JSON", "application/json")
            .put("JSONSCHEMA", "application/json")
            .put("TEXT", "text/plain")
            .put("XML", "application/xml")
            .build();

    protected static Map<String, String> SCHEMA_FORMAT_LOOKUP_MAP = ImmutableMap.<String, String>builder()
            .put("AVRO", "application/vnd.apache.avro;version=1.9.0")
            .put("JSONSCHEMA", "application/vnd.aai.asyncapi+json;version=2.0.0")
            .build();

    protected static String asString(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected static boolean isJSONValid(String jsonInString ) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(jsonInString);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
