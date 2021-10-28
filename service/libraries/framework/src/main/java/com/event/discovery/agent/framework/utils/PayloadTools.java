package com.event.discovery.agent.framework.utils;

import com.event.discovery.agent.framework.model.SchemaType;
import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.ArrayUtils;

public class PayloadTools {

    public static boolean isJson(String payload) {
        return isJson(payload.getBytes(Charsets.UTF_8));
    }

    public static boolean isJson(byte[] payload) {
        if (ArrayUtils.isNotEmpty(payload)) {
            // Check if first non-whitespace character is a {
            for (int idx = 0; idx < Math.min(10, payload.length); idx++) {
                if (!Character.isWhitespace(payload[idx])) {
                    return payload[idx] == '{';
                }
            }
        }
        return false;
    }

    public static boolean isXml(byte[] payload) {
        // Check if first non-whitespace character is a {
        for (int idx = 0; idx < Math.min(10, payload.length); idx++) {
            if (!Character.isWhitespace(payload[idx])) {
                return payload[idx] == '<';
            }
        }
        return false;
    }

    public static boolean isBinary(byte[] payload) {
        for (int idx = 0; idx < payload.length; idx++) {
            byte currentChar = payload[idx];
            if (currentChar < 32 && !Character.isWhitespace(currentChar)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isText(byte[] payload) {
        return !isBinary(payload);
    }

    public static SchemaType getSchemaType(byte[] payload) {
        if (payload != null && payload.length > 0) {
            if (isJson(payload)) {
                return SchemaType.JSONSCHEMA;
            } else if (isXml(payload)) {
                return SchemaType.XMLSCHEMA;
            } else if (isBinary(payload)) {
                return SchemaType.BINARY;
            } else if (isText(payload)) {
                return SchemaType.TEXT;
            }
        }
        return SchemaType.UNKNOWN;
    }

}
