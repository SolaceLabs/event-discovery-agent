package com.event.discovery.agent.rest.model;

import com.event.discovery.agent.framework.model.DTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Deprecated
public class EventDiscoveryOperationRequestDTO implements DTO {
    private String id;
    private String operationType;
    private List<String> subscriptions;
    private int duration;

    @Override
    public String getType() {
        return "eventListenerOperation";
    }

    public void setType(String type) {
        // intentionally left blank
    }

}
