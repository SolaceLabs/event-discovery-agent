package com.event.discovery.agent.task;

public interface TaskRunnerJob {

    void start();

    default void stop() {
        // NOOP
    }

    default void cancel() {
        // NOOP
    }
}
