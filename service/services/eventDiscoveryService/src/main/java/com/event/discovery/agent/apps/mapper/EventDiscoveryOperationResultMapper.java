package com.event.discovery.agent.apps.mapper;

import com.event.discovery.agent.apps.model.ApplicationToTopicBO;
import com.event.discovery.agent.apps.model.EventDiscoveryOperationDiscoveryResultBO;
import com.event.discovery.agent.apps.model.EventDiscoveryOperationResultBO;
import com.event.discovery.agent.rest.model.ApplicationToTopicDTO;
import com.event.discovery.agent.rest.model.EventDiscoveryOperationDiscoveryResultDTO;
import com.event.discovery.agent.rest.model.EventDiscoveryOperationResultDTO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface EventDiscoveryOperationResultMapper {
    List<String> passwordFields = Arrays.asList("adminPassword", "consumerPassword", "trustStorePassword",
            "keyStorePassword", "keyPassword", "basicAuthPassword", "sempPassword", "clientPassword",
            "clientHost", "sempHost");
    String OBFUSCATED_CREDENTIAL = "**************";

    EventDiscoveryOperationResultBO map(EventDiscoveryOperationResultDTO result);

    EventDiscoveryOperationResultDTO map(EventDiscoveryOperationResultBO result);

    EventDiscoveryOperationDiscoveryResultDTO mapToDTO(EventDiscoveryOperationDiscoveryResultBO result);

    ApplicationToTopicDTO map(ApplicationToTopicBO applicationToTopic);

    @AfterMapping
    default void obfuscatePasswords(@MappingTarget EventDiscoveryOperationDiscoveryResultDTO result) {
        obfuscatePasswords(result.getPluginInputs());
    }

    default void obfuscatePasswords(Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            if (passwordFields.contains(entry.getKey())) {
                if (entry.getValue() instanceof String) {
                    entry.setValue(OBFUSCATED_CREDENTIAL);
                }
            }
            if (entry.getValue() instanceof Map) {
                obfuscatePasswords((Map<String, Object>) entry.getValue());
            }
        }
    }

}
