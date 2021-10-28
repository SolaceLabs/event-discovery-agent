package com.event.discovery.agent.rest.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDTO {

    private String message;
    private String subCode;
    private String errorId;

    private Map<String, Object> meta;

    private Map<String, List<String>> validationDetails;

    public ErrorDTO(String message, String errorId) {
        this.message = message;
        this.errorId = errorId;
    }

}
