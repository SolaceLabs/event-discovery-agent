package com.event.discovery.agent.framework.utils;

public abstract class StoppableRunnable implements Runnable {
    protected volatile boolean exit = false;

    public void stop() {
        exit = true;
    }
}
