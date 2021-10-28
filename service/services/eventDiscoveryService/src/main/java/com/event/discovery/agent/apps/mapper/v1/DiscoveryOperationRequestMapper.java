package com.event.discovery.agent.apps.mapper.v1;

import com.event.discovery.agent.apps.model.v1.DiscoveryOperationRequestBO;
import com.event.discovery.agent.job.model.JobBO;
import com.event.discovery.agent.rest.model.JobDTO;
import com.event.discovery.agent.rest.model.v1.DiscoveryOperationRequestDTO;
import com.event.discovery.agent.rest.model.v1.DiscoveryOperationResponseDTO;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaAuthentication;
import com.event.discovery.agent.kafkaConfluent.model.ConfluentBrokerAuthentication;
import com.event.discovery.agent.solace.model.broker.SolaceBrokerAuthentication;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface DiscoveryOperationRequestMapper {
    ApacheKafkaAuthenticationMapper kafkaAuthenticationMapper = Mappers.getMapper(ApacheKafkaAuthenticationMapper.class);
    SolaceAuthenticationMapper solaceAuthenticationMapper = Mappers.getMapper(SolaceAuthenticationMapper.class);

    DiscoveryOperationRequestDTO map(DiscoveryOperationRequestBO operation);

    DiscoveryOperationRequestBO map(DiscoveryOperationRequestDTO operation);

    DiscoveryOperationResponseDTO mapToResponse(DiscoveryOperationRequestDTO operation);

    JobDTO map(JobBO job);

    @AfterMapping
    default DiscoveryOperationResponseDTO obfuscatePasswords(@MappingTarget DiscoveryOperationResponseDTO request) {
        if (request.getBrokerAuthentication() instanceof ApacheKafkaAuthentication) {
            request.setBrokerAuthentication(
                    kafkaAuthenticationMapper.mapKafkaCredentials((ApacheKafkaAuthentication) request.getBrokerAuthentication()));
        }

        if (request.getBrokerAuthentication() instanceof ConfluentBrokerAuthentication) {
            ConfluentBrokerAuthentication confluentBrokerAuthentication =
                    (ConfluentBrokerAuthentication) request.getBrokerAuthentication();
            confluentBrokerAuthentication.setSchemaRegistry(
                    kafkaAuthenticationMapper.mapServiceCredentials(confluentBrokerAuthentication.getSchemaRegistry()));
        }

        if (request.getBrokerAuthentication() instanceof SolaceBrokerAuthentication) {
            request.setBrokerAuthentication(
                    solaceAuthenticationMapper.mapSolaceCredentials((SolaceBrokerAuthentication) request.getBrokerAuthentication()));
        }
        return request;
    }
}