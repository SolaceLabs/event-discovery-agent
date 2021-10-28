package com.event.discovery.agent.rest.model;

import lombok.Data;

@Data
@Deprecated
public class EventDiscoveryOperationResponseDTO extends EventDiscoveryOperationRequestDTO {
    private String jobId;
}
