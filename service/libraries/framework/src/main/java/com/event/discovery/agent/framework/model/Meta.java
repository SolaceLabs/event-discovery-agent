package com.event.discovery.agent.framework.model;

import lombok.Data;

@Data
public class Meta {
    private Request request;
    private Integer responseCode;
    private Paging paging;
}
