package com.event.discovery.agent.rest.model;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;

public class RawJson {
        private String payload;

        public RawJson(String payload) {
            this.payload = payload;
        }

        public static RawJson from(String payload) {
            return new RawJson(payload);
        }

        @JsonValue
        @JsonRawValue
        public String getPayload() {
            return this.payload;
        }
}
