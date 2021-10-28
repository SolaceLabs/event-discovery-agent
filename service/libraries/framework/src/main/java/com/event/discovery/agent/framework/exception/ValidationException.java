package com.event.discovery.agent.framework.exception;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ValidationException extends EventDiscoveryAgentException {
    private static final long serialVersionUID = -5510535032099426548L;
    private String entityType;
    private Map<String, Collection<String>> errors;

    public ValidationException(String entityType, Map<String, Collection<String>> errors) {
        super(DiscoverySupportCode.VALIDATION_ERROR_102, "An entity of type " + entityType + " was passed in an invalid format");
        this.entityType = entityType;
        this.errors = errors;
    }

    public ValidationException(String entityType, String errorField, String errorMessage) {
        super(DiscoverySupportCode.VALIDATION_ERROR_102, "An entity of type " + entityType + " was passed in an invalid format");
        this.entityType = entityType;
        errors = new LinkedHashMap();
        errors.put(errorField, Collections.singletonList(errorMessage));
    }

    public ValidationException(String exceptionMessage, String entityType, String errorField, String errorMessage) {
        super(DiscoverySupportCode.VALIDATION_ERROR_102, exceptionMessage);
        this.entityType = entityType;
        errors = new LinkedHashMap();
        errors.put(errorField, Collections.singletonList(errorMessage));
    }

    public ValidationException(SupportCode supportCode, String exceptionMessage, String entityType, String errorField, String errorMessage) {
        super(supportCode, exceptionMessage);
        this.entityType = entityType;
        errors = new LinkedHashMap();
        errors.put(errorField, Collections.singletonList(errorMessage));
    }

    public String getEntityType() {
        return entityType;
    }

    public Map<String, Collection<String>> getErrors() {
        return errors;
    }
}
