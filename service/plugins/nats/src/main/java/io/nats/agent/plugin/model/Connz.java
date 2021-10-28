package io.nats.agent.plugin.model;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class Connz {
    private String server_id;
    private int num_connections;
    private int total;
    private int offset;
    private int limit;
    private Set<NATSConnection> connections = new HashSet<>();
}
