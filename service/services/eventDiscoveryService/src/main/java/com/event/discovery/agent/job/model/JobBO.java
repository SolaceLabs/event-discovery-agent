package com.event.discovery.agent.job.model;

import com.event.discovery.agent.framework.model.JobStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class JobBO {
    private String id;
    private JobStatus status;
}
