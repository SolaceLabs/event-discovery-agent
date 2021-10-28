package com.event.discovery.agent.framework.task;

import com.event.discovery.agent.framework.exception.EventDiscoveryAgentException;
import com.event.discovery.agent.framework.model.AppStatus;
import com.event.discovery.agent.framework.model.DiscoveryAppSb;
import com.event.discovery.agent.framework.model.Job;
import com.event.discovery.agent.framework.model.JobStatus;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class RunnableJobTests {

    @Test
    public void runTest() {
        DiscoveryApp app = new DiscoveryApp();
        Job job = new Job("1", null, null);
        AtomicReference<Boolean> wasRun = new AtomicReference<>(false);
        RunnableJob runnableJob = new RunnableJob(app, job, () -> {
            wasRun.set(true);
        });

        runnableJob.run();
        assertThat(wasRun.get(), is(Boolean.TRUE));
        assertThat(app.status, nullValue());
        assertThat(job.getStatus(), nullValue());
    }

    @Test
    public void exceptionTest() {
        DiscoveryApp app = new DiscoveryApp();
        Job job = new Job("1", null, null);
        RunnableJob runnableJob = new RunnableJob(app, job, () -> {
            throw new IllegalArgumentException();
        });

        Exception exception = null;
        try {
            runnableJob.run();
        } catch (Exception e) {
            exception = e;
        }

        assertThat(exception instanceof EventDiscoveryAgentException, is(true));
        assertThat(app.status, is(AppStatus.ERROR));
        assertThat(job.getStatus(), is(JobStatus.ERROR));
    }

    class DiscoveryApp implements DiscoveryAppSb {
        public AppStatus status;

        @Override
        public void updateAppStatus(AppStatus appStatus) {
            status = appStatus;
        }

        @Override
        public void setError(String error) {
        }
    }
}
