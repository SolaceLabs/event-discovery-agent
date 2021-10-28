package com.event.discovery.agent.solace.model.broker;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.event.discovery.agent.framework.model.broker.BrokerAuthentication;
import com.event.discovery.agent.framework.model.broker.NoAuthBrokerAuthentication;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonDeserialize(as = SolaceBrokerAuthentication.class)
public class SolaceBrokerAuthentication extends NoAuthBrokerAuthentication implements BrokerAuthentication {
    private String clientUsername;
    private String clientPassword;
    private String sempUsername;
    private String sempPassword;
    private String keyStoreLocation;
    private String keyStorePassword;
    private String keyPassword;
    private String trustStoreLocation;
    private String trustStorePassword;
}