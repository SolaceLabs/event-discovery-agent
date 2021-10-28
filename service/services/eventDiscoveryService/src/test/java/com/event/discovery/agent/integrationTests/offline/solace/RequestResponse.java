package com.event.discovery.agent.integrationTests.offline.solace;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class RequestResponse {
    private JsonNode request;
    private JsonNode response;
}
