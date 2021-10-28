package com.event.discovery.agent.framework.model.messages;

import com.event.discovery.agent.framework.model.NormalizedMsg;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class BasicNormalizedMessage implements NormalizedMsg {
    private byte[] payload;
    private String contentType;

    // Message meta data
    private long messageSize;
    private long sentTimestamp;
    private long receivedTimestamp;
}
