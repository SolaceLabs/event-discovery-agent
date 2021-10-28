package com.event.discovery.agent.framework.exception;

public class EntityNotFoundException extends EventDiscoveryAgentException {
    private String entityType;
    private String entityLabel;

    public EntityNotFoundException(String entityType, String entityLabel) {
        super(DiscoverySupportCode.NOT_FOUND_ERROR_103, "Could not find " + entityType + " " + entityLabel);
        this.entityType = entityType;
        this.entityLabel = entityLabel;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityLabel() {
        return entityLabel;
    }
}
