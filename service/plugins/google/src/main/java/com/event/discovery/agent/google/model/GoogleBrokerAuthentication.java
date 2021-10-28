package com.event.discovery.agent.google.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.event.discovery.agent.framework.model.broker.BrokerAuthentication;
import com.event.discovery.agent.framework.model.broker.NoAuthBrokerAuthentication;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonDeserialize(as = GoogleBrokerAuthentication.class)
public class GoogleBrokerAuthentication extends NoAuthBrokerAuthentication implements BrokerAuthentication {
    private String credentials;
    private String projectId;
}