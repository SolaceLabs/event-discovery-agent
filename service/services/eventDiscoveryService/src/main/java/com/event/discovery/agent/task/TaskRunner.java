package com.event.discovery.agent.task;

import com.google.common.collect.Lists;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;

public class TaskRunner {

    private final List<TaskRunnerJob> tasks = new ArrayList<>();
    private final ThreadPoolTaskExecutor executor;

    public TaskRunner() {
        executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("EDATR-");
        executor.setCorePoolSize(20);
        executor.initialize();
    }

    public TaskRunner add(final TaskRunnerJob taskRunnerJob) {
        tasks.add(taskRunnerJob);
        return this;
    }

    /**
     * This starts up the added tasks in reverse order. The idea here is to add the processes in logical order
     * and then start them in reverse order to insure nothing is lost between them.
     */
    public void start() {
        Lists.reverse(tasks).forEach(t -> executor.execute(t::start));
    }

    /**
     * This stops the tasks in the order which they were added
     */
    public void stop() {
        tasks.forEach(t -> {
            try {
                t.stop();
            } catch (final Exception ignored) {
            }
        });
        executor.shutdown();
    }

    public void cancel() {
        stop();
    }

    public static TaskRunner create() {
        return new TaskRunner();
    }
}
