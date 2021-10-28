package com.event.discovery.agent.base.config;

import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
public class JobProperties {
    private Long cacheDuration;
    private TimeUnit cacheDurationUnit;
    private int cacheSize;
}
