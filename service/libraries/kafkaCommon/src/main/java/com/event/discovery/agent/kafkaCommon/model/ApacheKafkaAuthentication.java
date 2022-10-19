package com.event.discovery.agent.kafkaCommon.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.event.discovery.agent.framework.model.ServiceAuthentication;
import com.event.discovery.agent.framework.model.broker.BrokerAuthentication;
import com.event.discovery.agent.framework.model.broker.NoAuthBrokerAuthentication;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonDeserialize(as = ApacheKafkaAuthentication.class)
public class ApacheKafkaAuthentication extends NoAuthBrokerAuthentication implements BrokerAuthentication {
    private KafkaAuthentiationType authenticationType;
    private KafkaTransportType transportType;

    // SASL_PLAIN & SASL SCRAM parameters
    private String consumerUsername;
    private String consumerPassword;
    private String adminUsername;
    private String adminPassword;

    // SSL Parameters
    private String trustStoreLocation;
    private String trustStorePassword;
    private String keyStoreLocation;
    private String keyStorePassword;
    private String keyPassword;
    private Boolean sslNoVerify;

    // SASL_GSSAPI
    private String principal;
    private String keyTabLocation;
    private String krb5ConfigurationLocation;
    private String serviceName;

    private ServiceAuthentication connector;

    public enum KafkaAuthentiationType {
        NOAUTH, SASL_PLAIN, SASL_GSSAPI, SASL_SCRAM_256, SASL_SCRAM_512
    }

    public enum KafkaTransportType {
        PLAINTEXT, SSL
    }
}
