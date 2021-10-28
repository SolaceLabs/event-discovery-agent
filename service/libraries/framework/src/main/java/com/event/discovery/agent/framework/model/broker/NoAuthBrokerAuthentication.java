package com.event.discovery.agent.framework.model.broker;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonDeserialize(as = NoAuthBrokerAuthentication.class)
public class NoAuthBrokerAuthentication implements BrokerAuthentication {
    private String brokerType;
}
