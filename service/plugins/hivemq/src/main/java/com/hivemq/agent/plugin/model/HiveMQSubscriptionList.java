package com.hivemq.agent.plugin.model;

import lombok.Data;

import java.util.List;

@Data
public class HiveMQSubscriptionList {
    private List<HiveMQSubscription> items;
}
