package com.event.discovery.agent.kafkaCommon.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RawKafkaRecord {
    private byte[] key;
    private byte[] value;
    private String topic;
    private long createdTimestamp;
    private long receivedTimestamp;
}
