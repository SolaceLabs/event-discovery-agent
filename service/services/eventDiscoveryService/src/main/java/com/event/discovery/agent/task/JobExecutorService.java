package com.event.discovery.agent.task;

import com.event.discovery.agent.framework.model.DiscoveryAppSb;
import com.event.discovery.agent.framework.model.Job;
import com.event.discovery.agent.framework.task.RunnableJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class JobExecutorService {
    private final TaskExecutor taskExecutor;

    @Autowired
    public JobExecutorService(@Qualifier("eventDiscoveryAgentTaskExecutor") final TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void execute(final DiscoveryAppSb discoveryAppSb, final Job job, final Runnable runnable) {
        final RunnableJob runnableJob = new RunnableJob(discoveryAppSb, job, runnable);
        getTaskExecutor().execute(runnableJob);
    }

    public TaskExecutor getTaskExecutor() {
        return taskExecutor;
    }
}
