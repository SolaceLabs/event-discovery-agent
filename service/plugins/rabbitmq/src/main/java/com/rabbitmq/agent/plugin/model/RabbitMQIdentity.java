package com.rabbitmq.agent.plugin.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.event.discovery.agent.framework.model.broker.BasicBrokerIdentity;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonDeserialize(as = RabbitMQIdentity.class)
public class RabbitMQIdentity extends BasicBrokerIdentity implements BrokerIdentity {
    private String hostname;
    private String clientProtocol;
    private String clientPort;
    private String adminPort;
    private String clientUsername;
    private String clientPassword;


    @Override
    public String getBrokerType() {
        return "RABBITMQ";
    }
}
