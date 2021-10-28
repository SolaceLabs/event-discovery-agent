package com.event.discovery.agent.framework.model;

import lombok.Data;

@Data
public class Paging {
    private String cursorQuery;
    private String nextPageUri;
}
