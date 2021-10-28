package com.event.discovery.agent.job.model;


import com.event.discovery.agent.apps.model.DiscoveryApp;
import com.event.discovery.agent.framework.model.Job;
import com.event.discovery.agent.framework.model.JobStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApplicationJob extends Job {
    private DiscoveryApp app;

    public ApplicationJob(String id, JobStatus status) {
        super(id, status, null);
    }


}
