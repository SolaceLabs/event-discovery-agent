package com.event.discovery.agent.apps.mapper.v1;

import com.event.discovery.agent.framework.model.ServiceAuthentication;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaAuthentication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ApacheKafkaAuthenticationMapper {
    String OBFUSCATED_CREDENTIAL = "**************";

    @Mapping(source = "adminPassword", target = "adminPassword", qualifiedByName = "obfuscator")
    @Mapping(source = "consumerPassword", target = "consumerPassword", qualifiedByName = "obfuscator")
    @Mapping(source = "trustStorePassword", target = "trustStorePassword", qualifiedByName = "obfuscator")
    @Mapping(source = "keyStorePassword", target = "keyStorePassword", qualifiedByName = "obfuscator")
    @Mapping(source = "keyPassword", target = "keyPassword", qualifiedByName = "obfuscator")
    @Mapping(source = "connector.trustStorePassword", target = "connector.trustStorePassword", qualifiedByName = "obfuscator")
    @Mapping(source = "connector.keyStorePassword", target = "connector.keyStorePassword", qualifiedByName = "obfuscator")
    @Mapping(source = "connector.basicAuthPassword", target = "connector.basicAuthPassword", qualifiedByName = "obfuscator")
    ApacheKafkaAuthentication mapKafkaCredentials(ApacheKafkaAuthentication source);

    @Mapping(source = "trustStorePassword", target = "trustStorePassword", qualifiedByName = "obfuscator")
    @Mapping(source = "keyStorePassword", target = "keyStorePassword", qualifiedByName = "obfuscator")
    @Mapping(source = "basicAuthPassword", target = "basicAuthPassword", qualifiedByName = "obfuscator")
    ServiceAuthentication mapServiceCredentials(ServiceAuthentication source);

    @Named("obfuscator")
    default String obfuscateCredentials(String input) {
        return input != null ? OBFUSCATED_CREDENTIAL : null;
    }

}
