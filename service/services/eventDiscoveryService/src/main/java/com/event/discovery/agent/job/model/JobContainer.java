package com.event.discovery.agent.job.model;

import com.event.discovery.agent.springboot.properties.DiscoveryProperties;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.event.discovery.agent.framework.model.Job;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JobContainer {
    /**
     * Guava cache has been chosen as an underlying data structure b/c it implements thread-safe concurrent access.
     * Due to internal synchronization object publishing is safe (threads see up-to-date object without external synchronization).
     * Such cache also expires and purges records automatically. Otherwise, we'd have to implement a timer-task to achieve it.
     * <p>
     * NOTE: this implementation will yield satisfactory results only(!) in non-clustered deployment.
     * Otherwise, we'd have to use an external storage (distributed cache || relational db).
     */
    private final Cache<String, Job> jobHolder;

    public JobContainer(DiscoveryProperties discoveryProperties) {
        jobHolder = CacheBuilder.newBuilder().maximumSize(discoveryProperties.getJobProperties().getCacheSize())
                .expireAfterWrite(discoveryProperties.getJobProperties().getCacheDuration(),
                        discoveryProperties.getJobProperties().getCacheDurationUnit())
                .build();
    }

    @SuppressWarnings("unchecked")
    public <T extends Job> Optional<T> get(String jobId) {
        T job = (T) jobHolder.getIfPresent(jobId);
        return Optional.ofNullable(job);
    }

    public <T extends Job> void put(T job) {
        jobHolder.put(job.getId(), job);
    }

    public void remove(String jobId) {
        jobHolder.invalidate(jobId);
    }

}
