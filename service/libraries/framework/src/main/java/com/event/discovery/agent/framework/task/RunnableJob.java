package com.event.discovery.agent.framework.task;

import com.event.discovery.agent.framework.exception.DiscoverySupportCode;
import com.event.discovery.agent.framework.exception.EventDiscoveryAgentException;
import com.event.discovery.agent.framework.model.AppStatus;
import com.event.discovery.agent.framework.model.DiscoveryAppSb;
import com.event.discovery.agent.framework.model.Job;
import com.event.discovery.agent.framework.model.JobStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RunnableJob implements Runnable {
    private static final String FAILED_TO_CREATE_ADMIN_CLIENT = "Failed to create new KafkaAdminClient";
    private final Job job;
    private final DiscoveryAppSb discoveryAppSb;
    private final Runnable task;

    public RunnableJob(final DiscoveryAppSb discoveryAppSb, final Job job, final Runnable task) {
        this.discoveryAppSb = discoveryAppSb;
        this.job = job;
        this.task = task;
    }

    public Job getJob() {
        return job;
    }

    public DiscoveryAppSb getDiscoveryAppSb() {
        return discoveryAppSb;
    }

    public Runnable getTask() {
        return task;
    }

    @Override
    public void run() {
        try {
            getTask().run();
        } catch (final Exception e) {
            handleException(e);
        }
    }

    protected void handleException(final Exception e) {
        String errorMessage = handleErrorMessage(e);
        getJob().setStatus(JobStatus.ERROR);
        getJob().setError(errorMessage);
        getDiscoveryAppSb().updateAppStatus(AppStatus.ERROR);
        getDiscoveryAppSb().setError(errorMessage);
        log.error("Unable to run job: " + e.getMessage(), e);
        // errors will always exit the runnable
        throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101, e.getMessage());
    }

    private String handleErrorMessage(final Exception e) {
        if (e.getMessage() == null) {
            return "No error message was provided.";
        }
        if (e.getMessage().endsWith(FAILED_TO_CREATE_ADMIN_CLIENT)) {
            return FAILED_TO_CREATE_ADMIN_CLIENT;
        }
        return e.getMessage();
    }
}
