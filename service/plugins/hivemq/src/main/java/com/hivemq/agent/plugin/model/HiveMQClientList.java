package com.hivemq.agent.plugin.model;

import lombok.Data;

import java.util.List;

@Data
public class HiveMQClientList {
    private List<HiveMQClient> items;
}
