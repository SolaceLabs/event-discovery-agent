package com.event.discovery.agent.framework.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Iterator;

@Slf4j
@Component
public final class JsonSchemaGenerator {

    private final ObjectMapper objectMapper;

    @Autowired
    public JsonSchemaGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String outputAsString(String title, String json) throws IOException {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(json);
        // log.info(gson.toJson(je));
        try {
            JSONObject jsonObject = new JSONObject(json);
            je = jp.parse(jsonObject.toString());
            log.debug(gson.toJson(je));
        } catch (Exception err) {
            log.error("Error", err.toString());
        }

        String result = outputAsString(title, json, null, null, null, null);
        log.info(result);
        gson = new GsonBuilder().setPrettyPrinting().create();
        jp = new JsonParser();
        je = jp.parse(result);
        return gson.toJson(je);

    }

    private String outputAsString(String title, String json, JsonNodeType type, String previousName,
                                  String path, String previousPath) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(json);
        StringBuilder output = new StringBuilder();
        output.append('{');

        if (type == null) {
            output.append(
                    "\"definitions\": {}, \"$schema\": \"http://json-schema.org/draft-07/schema#\", \"type\": \"object\", \"title\": \""
                            + title + "\",\"properties\": {");
            path = "#/properties";
            previousPath = path;
        }

        for (Iterator<String> iterator = jsonNode.fieldNames();
             iterator.hasNext();
        ) {
            String fieldName = iterator.next();
            log.info("Processing fieldName: " + fieldName);

            if (fieldName.equals("list")) {
                System.out.println();
            }
            if (iterator.hasNext() || type == null || type != null && type.name().equals(JsonNodeType.OBJECT.name()) && !fieldName.equals(previousName)) {
                path = previousPath;
            }
            if (type != null && type.name().equals(JsonNodeType.OBJECT.name())) {
                previousName = fieldName;
            }

            path += "/" + fieldName;
            JsonNodeType nodeType = jsonNode.get(fieldName).getNodeType();

            output.append(convertNodeToStringSchemaNode(jsonNode, nodeType, fieldName, path, previousName, iterator.hasNext()));
        }

        if (type == null) {
            output.append('}');
        }
        output.append('}');
        return output.toString().equals("{}") ? "" : output.toString();
    }

    private String convertNodeToStringSchemaNode(JsonNode jsonNode, JsonNodeType nodeType, String key, String path,
                                                 String previousName, boolean hasNext) throws IOException {
        StringBuilder result = new StringBuilder("\"" + key + "\": { \"$id\": \"" + path + "\", \"title\": \"" + key + " schema\", \"type\": ");
        log.info(key + " node type " + nodeType + " with value " + jsonNode.get(key));
        JsonNode node;
        switch (nodeType) {
            case ARRAY:
                node = jsonNode.get(key).get(0);
                if (node != null) {
                    JsonNodeType jsonNodeType = getNodeType(node);
                    result.append("\"array\", \"items\": {  \"$id\": \"" + path + "/items\", \"title\": \" items schema \", \"type\": \""
                            + jsonNodeType.name().toLowerCase() + "\"");
                    path += "/";
                    if (jsonNodeType.equals(JsonNodeType.OBJECT)) {
                        path += "items/properties";
                        result.append(',').append("\"properties\": ");
                    } else {
                        path += node;
                    }
                    result.append(outputAsString(key + " schema", node.toString(), JsonNodeType.ARRAY, "", path, hasNext ? path : trimPath(path)));
                    result.append(hasNext ? "}}," : "}}");
                } else {
                    result.append("\"array\"");
                    result.append(hasNext ? "}," : "}");
                    log.error("Empty list");
                }
//                if (jsonNodeType.equals(JsonNodeType.OBJECT) || jsonNodeType.equals(JsonNodeType.ARRAY)) {
//                    // result.append("}");
//                }
                break;
            case BOOLEAN:
                addBracketsAndCommas(result, "boolean\"", hasNext);
                break;
            case NUMBER:
                addBracketsAndCommas(result, "number\"", hasNext);
                break;
            case OBJECT:
                node = jsonNode.get(key);
                result.append("\"object\", \"properties\": ");
                String tempPath = path + "/";
                result.append(outputAsString(key + " schema", node.toString(), JsonNodeType.OBJECT, previousName, tempPath, hasNext ? path : trimPath(path)));
                result.append(hasNext ? "}," : "}");
                break;
            case STRING:
                addBracketsAndCommas(result, "string\"", hasNext);
                break;
            case NULL:
                addBracketsAndCommas(result, "null\"", hasNext);
                break;
            case POJO:
                log.warn("Not handled");
                // not handled
                break;
            case BINARY:
                addBracketsAndCommas(result, "binary\"", hasNext);
                break;
            default:
                break;
        }

        return result.toString();
    }

    private static void addBracketsAndCommas(StringBuilder result, String element, boolean hasNext) {
        result.append('\"').append(element);
        if (hasNext) {
            result.append("},");
        } else {
            result.append('}');
        }
    }

    private static String trimPath(String path) {
        return path.substring(0, path.lastIndexOf('/'));
    }

    private static JsonNodeType getNodeType(JsonNode node) {
        JsonNodeType jsonNodeType = JsonNodeType.MISSING;
        if (node.getNodeType() != null) {
            switch (node.getNodeType()) {
                case ARRAY:
                    jsonNodeType = JsonNodeType.ARRAY;
                    break;
                case BOOLEAN:
                    jsonNodeType = JsonNodeType.BOOLEAN;
                    break;
                case NUMBER:
                    jsonNodeType = JsonNodeType.NUMBER;
                    break;
                case OBJECT:
                    jsonNodeType = JsonNodeType.OBJECT;
                    break;
                case STRING:
                    jsonNodeType = JsonNodeType.STRING;
                    break;
                case NULL:
                    jsonNodeType = JsonNodeType.NULL;
                    break;
                case POJO:
                    jsonNodeType = JsonNodeType.POJO;
                    break;
                case BINARY:
                    jsonNodeType = JsonNodeType.BINARY;
                    break;
                default:
                    break;
            }
        }
        return jsonNodeType;
    }
}