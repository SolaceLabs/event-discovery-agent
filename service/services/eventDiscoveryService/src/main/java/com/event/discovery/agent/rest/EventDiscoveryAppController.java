package com.event.discovery.agent.rest;

import com.event.discovery.agent.apps.mapper.EventDiscoveryOperationRequestMapper;
import com.event.discovery.agent.rest.model.RawJson;
import com.event.discovery.agent.apps.model.EventDiscoveryOperationDiscoveryResultBO;
import com.event.discovery.agent.apps.model.EventDiscoveryOperationResultBO;
import com.event.discovery.agent.asyncapi.AsyncApiManager;
import com.event.discovery.agent.job.model.JobBO;
import com.event.discovery.agent.managers.AppsManager;
import com.event.discovery.agent.rest.model.JobDTO;
import com.event.discovery.agent.springboot.properties.DiscoveryProperties;
import com.event.discovery.agent.framework.model.EnvelopeDTO;
import com.event.discovery.agent.framework.model.Job;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

@RestController("EventDiscoveryAppController")
@RequestMapping("/api/v0/event-discovery-agent/${event.discovery.id}/app/operation")
@Validated
@Slf4j
public class EventDiscoveryAppController extends EventDiscoveryControllerBase {

    private final AppsManager appsManager;
    private final EventDiscoveryOperationRequestMapper eventDiscoveryOperationRequestMapper;
    private final DiscoveryProperties discoveryProperties;
    private final AsyncApiManager asyncApiManager;

    @Autowired
    public EventDiscoveryAppController(AppsManager appsManager, DiscoveryProperties discoveryProperties,
                                       EventDiscoveryOperationRequestMapper eventDiscoveryOperationRequestMapper,
                                       AsyncApiManager asyncApiManager) {
        super();
        this.appsManager = appsManager;
        this.eventDiscoveryOperationRequestMapper = eventDiscoveryOperationRequestMapper;
        this.discoveryProperties = discoveryProperties;
        this.asyncApiManager = asyncApiManager;
    }

    @GetMapping(path = "/{jobId}/result/asyncapi", produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RawJson> getAsyncAPIOutput(
            @NotBlank @PathVariable("jobId") String jobId,
            @RequestParam(value = "version", defaultValue = "2.2", required = false) String version) {
        log.debug("getAsyncAPIOutput - Getting job result for {}", jobId);
        EventDiscoveryOperationResultBO scanResult = appsManager.getAppResults(jobId);
        JSONObject asyncApiSpec = new JSONObject();
        if (scanResult instanceof EventDiscoveryOperationDiscoveryResultBO) {
            asyncApiSpec = asyncApiManager.getAsyncApi(version, (EventDiscoveryOperationDiscoveryResultBO) scanResult);
        }
        return ResponseEntity.ok(RawJson.from(asyncApiSpec.toString()));
    }

    @GetMapping(path = "/{jobId}/status")
    public ResponseEntity<EnvelopeDTO<JobDTO>> getJobStatus(@NotBlank @PathVariable("jobId") String jobId) {
        log.debug("getJobStatus - Getting job status for {}", jobId);
        Job job = appsManager.getJob(jobId);
        JobDTO response = JobDTO.builder()
                .id(jobId)
                .status(job.getStatus())
                .error(job.getError())
                .build();
        return ResponseEntity.ok(new EnvelopeDTO<>(response));
    }

    @PostMapping(path = "/{jobId}/stop")
    public ResponseEntity<EnvelopeDTO<JobDTO>> stopApp(@NotBlank @PathVariable("jobId") String jobId) {
        log.debug("stopApp - Stopping job {}", jobId);
        JobBO job = appsManager.stopApp(jobId);
        return ResponseEntity.ok(new EnvelopeDTO<>(eventDiscoveryOperationRequestMapper.map(job)));
    }

    @PostMapping(path = "/{jobId}/cancel")
    public ResponseEntity<EnvelopeDTO<JobDTO>> cancelApp(@NotBlank @PathVariable("jobId") String jobId) {
        log.debug("stopApp - Stopping job {}", jobId);
        JobBO job = appsManager.cancelApp(jobId);
        return ResponseEntity.ok(new EnvelopeDTO<>(eventDiscoveryOperationRequestMapper.map(job)));
    }

    @GetMapping(path = "/version")
    public ResponseEntity<Object> DiscoveryAgentVersion() {
        log.debug("getting current discovery agent version");
        return ResponseEntity.ok(discoveryProperties.getVersion().toString());
    }
}
