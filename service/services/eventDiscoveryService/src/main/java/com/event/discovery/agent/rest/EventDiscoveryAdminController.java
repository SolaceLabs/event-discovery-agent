package com.event.discovery.agent.rest;

import com.event.discovery.agent.apps.mapper.v1.DiscoveryOperationRequestMapper;
import com.event.discovery.agent.managers.AppsManager;
import com.event.discovery.agent.rest.model.v1.DiscoveryOperationRequestDTO;
import com.event.discovery.agent.rest.model.v1.DiscoveryOperationResponseDTO;
import com.event.discovery.agent.framework.model.EnvelopeDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController("EventDiscoveryAdminController")
@RequestMapping("/api/v0/event-discovery-agent/${event.discovery.id}")
@Validated
@Slf4j
public class EventDiscoveryAdminController extends EventDiscoveryControllerBase {

    private final AppsManager appsManager;
    private final DiscoveryOperationRequestMapper discoveryOperationRequestMapper;

    @Autowired
    public EventDiscoveryAdminController(AppsManager appsManager,
                                         DiscoveryOperationRequestMapper discoveryOperationRequestMapper) {
        super();
        this.appsManager = appsManager;
        this.discoveryOperationRequestMapper = discoveryOperationRequestMapper;
    }

    @PostMapping(path = "/app/operation")
    public ResponseEntity<EnvelopeDTO<DiscoveryOperationResponseDTO>> start(@RequestBody @Valid DiscoveryOperationRequestDTO discoveryOp) {
        log.debug("create runtime event discovery operation");
        log.info(discoveryOp.getDiscoveryOperation().getOperationType());
        String jobId = appsManager.startApp(discoveryOperationRequestMapper.map(discoveryOp));
        DiscoveryOperationResponseDTO response = discoveryOperationRequestMapper.mapToResponse(discoveryOp);
        response.setJobId(jobId);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.add(HttpHeaders.LOCATION, "/api/v0/event-discovery-agent/app/operation/" + jobId + "/status");

        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(new EnvelopeDTO<>(response));
    }
}
