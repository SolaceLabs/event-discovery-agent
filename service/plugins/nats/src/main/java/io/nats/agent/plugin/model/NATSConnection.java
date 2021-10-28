package io.nats.agent.plugin.model;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class NATSConnection {
    private int cid;
    private String type;
    private String name;
    private Set<String> subscriptions_list = new HashSet<>();
}
