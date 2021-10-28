package com.event.discovery.agent.kafkaCommon.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class KafkaConnectorTask {
    @JsonProperty("id")
    private Integer taskId;
    @JsonProperty("worker_id")
    private String workerId;
    @JsonProperty("state")
    private TaskState state;

    public enum TaskState {
        RUNNING("RUNNING"),
        FAILED("FAILED"),
        PAUSED("PAUSED"),
        UNASSIGNED("UNASSIGNED");

        private final String state;

        TaskState(String state) {
            this.state = state;
        }

        @JsonValue
        String getState() {
            return state;
        }
    }
}

