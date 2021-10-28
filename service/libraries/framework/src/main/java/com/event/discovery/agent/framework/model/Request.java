package com.event.discovery.agent.framework.model;

import lombok.Data;

@Data
public class Request {
    private String method;
    private String uri;
}
