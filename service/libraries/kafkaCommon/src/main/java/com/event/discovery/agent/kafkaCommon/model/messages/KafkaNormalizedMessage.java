package com.event.discovery.agent.kafkaCommon.model.messages;

import com.event.discovery.agent.framework.model.NormalizedMsg;

public interface KafkaNormalizedMessage extends NormalizedMsg {
    byte[] getKey();
}
