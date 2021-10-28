package com.event.discovery.agent.framework.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnvelopeDTO<T> {
    protected T data;
    protected Map<String, Object> meta;
    protected List<?> included;

    public EnvelopeDTO(T data) {
        this.data = data;
    }
}
