package com.event.discovery.agent.framework.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder(builderMethodName = "noArgsBuilder")
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    private String id;
    private volatile JobStatus status;
    private volatile String error;
    private final Map<String, Object> errorData = new ConcurrentHashMap<>();

    public static Job.JobBuilder jobBuilder(String id, JobStatus status) {
        return Job.noArgsBuilder().id(id).status(status);
    }

    public void appendErrorData(String key, Object value) {
        errorData.put(key, value);
    }

    public Map<String, Object> getErrorData() {
        return new HashMap<>(errorData);
    }

    public void setError(String error) {
        this.error = error;
    }
}
