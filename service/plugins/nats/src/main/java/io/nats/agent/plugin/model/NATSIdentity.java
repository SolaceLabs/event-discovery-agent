package io.nats.agent.plugin.model;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.event.discovery.agent.framework.model.broker.BasicBrokerIdentity;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonDeserialize(as = NATSIdentity.class)
public class NATSIdentity extends BasicBrokerIdentity implements BrokerIdentity {
    private String hostname = "localhost";
    private String clientProtocol = "nats";
    private int clientPort = 4222;
    private String adminPortocol = "http";
    private int adminPort = 6222;
}
