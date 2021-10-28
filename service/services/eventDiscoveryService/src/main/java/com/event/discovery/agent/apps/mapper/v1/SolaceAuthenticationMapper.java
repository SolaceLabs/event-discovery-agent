package com.event.discovery.agent.apps.mapper.v1;

import com.event.discovery.agent.solace.model.broker.SolaceBrokerAuthentication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface SolaceAuthenticationMapper {
    String OBFUSCATED_CREDENTIAL = "**************";

    @Mapping(source = "sempPassword", target = "sempPassword", qualifiedByName = "obfuscator")
    @Mapping(source = "clientPassword", target = "clientPassword", qualifiedByName = "obfuscator")
    @Mapping(source = "trustStorePassword", target = "trustStorePassword", qualifiedByName = "obfuscator")
    @Mapping(source = "keyStorePassword", target = "keyStorePassword", qualifiedByName = "obfuscator")
    @Mapping(source = "keyPassword", target = "keyPassword", qualifiedByName = "obfuscator")
    SolaceBrokerAuthentication mapSolaceCredentials(SolaceBrokerAuthentication source);

    @Named("obfuscator")
    default String obfuscateCredentials(String input) {
        return input != null ? OBFUSCATED_CREDENTIAL : null;
    }

}
