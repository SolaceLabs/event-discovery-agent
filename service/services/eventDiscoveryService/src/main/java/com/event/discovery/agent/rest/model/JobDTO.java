package com.event.discovery.agent.rest.model;

import com.event.discovery.agent.framework.model.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDTO {
    private String id;
    private JobStatus status;
    private String error;
}
