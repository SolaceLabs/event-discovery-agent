package com.event.discovery.agent.kafkaCommon.model.messages;

import com.event.discovery.agent.framework.model.messages.BasicNormalizedMessage;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class KafkaNormalizedMessageImpl extends BasicNormalizedMessage implements KafkaNormalizedMessage {
    private byte[] key;
}
