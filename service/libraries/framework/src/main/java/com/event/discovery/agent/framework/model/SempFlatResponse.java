package com.event.discovery.agent.framework.model;

import lombok.Data;

import java.util.Map;

@Data
public class SempFlatResponse<T> {
    private T data;
    private Map<Object, Object> links;
    private Meta meta;

}
