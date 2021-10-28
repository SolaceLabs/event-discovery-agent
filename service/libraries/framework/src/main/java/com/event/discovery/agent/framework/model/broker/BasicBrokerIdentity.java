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
@JsonDeserialize(as = BasicBrokerIdentity.class)
public class BasicBrokerIdentity implements BrokerIdentity {
    private String brokerType;
}
