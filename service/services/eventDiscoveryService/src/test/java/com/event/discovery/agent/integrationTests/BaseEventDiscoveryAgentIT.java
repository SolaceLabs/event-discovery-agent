package com.event.discovery.agent.integrationTests;

import com.event.discovery.agent.apps.model.EventDiscoveryOperationDiscoveryResultBO;
import com.event.discovery.agent.asyncapi.CommonModelToAsyncAPIMapper_2_2;
import com.event.discovery.agent.rest.model.JobDTO;
import com.event.discovery.agent.rest.model.v1.DiscoveryOperationRequestDTO;
import com.event.discovery.agent.rest.model.v1.DiscoveryOperationResponseDTO;
import com.event.discovery.agent.framework.model.JobStatus;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@Slf4j
public abstract class BaseEventDiscoveryAgentIT extends BaseIT {

    protected static final String APPLICATION_OPERATION_CREATE_URL = "/api/v0/event-discovery-agent/local-test/app/operation";
    protected static final String APPLICATION_OPERATION_STATUS_URL = "/api/v0/event-discovery-agent/local-test/app/operation/{jobId}/status";
    protected static final String APPLICATION_OPERATION_RESULT_ASYNCAPI_URL = "/api/v0/event-discovery-agent/local-test/app/operation/{jobId}/result/asyncapi";
    protected static final String APPLICATION_OPERATION_STOP_URL = "/api/v0/event-discovery-agent/local-test/app/operation/{jobId}/stop";

    @SpyBean
    protected CommonModelToAsyncAPIMapper_2_2 asyncApiMapper;

    protected DiscoveryOperationResponseDTO createEventListenerOperation(DiscoveryOperationRequestDTO eventListenerOperation) {

        DiscoveryOperationResponseDTO response = postAndThen(eventListenerOperation, APPLICATION_OPERATION_CREATE_URL)
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("Content-Type", "application/json")
                .body(
                        "data.brokerIdentity.brokerType", is(eventListenerOperation.getBrokerIdentity().getBrokerType()),
                        "data.brokerAuthentication.brokerType", is(eventListenerOperation.getBrokerAuthentication().getBrokerType()),
                        "data.discoveryOperation.operationType", is(eventListenerOperation.getDiscoveryOperation().getOperationType()),
                        "data.discoveryOperation.messageQueueLength", is(eventListenerOperation.getDiscoveryOperation().getMessageQueueLength()),
                        "data.discoveryOperation.durationInSecs", is(eventListenerOperation.getDiscoveryOperation().getDurationInSecs()),
                        "data.discoveryOperation.subscriptionSet",
                        containsInAnyOrder(eventListenerOperation.getDiscoveryOperation().getSubscriptionSet().toArray()),
                        "data.jobId", notNullValue()
                )
                .extract().jsonPath().getObject("data", DiscoveryOperationResponseDTO.class);

        return response;
    }

    public JobDTO getDiscoveryOperationStatus(String jobId) {
        return getAndThen(APPLICATION_OPERATION_STATUS_URL, jobId)
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("data.id", is(jobId),
                        "data.status", notNullValue())
                .extract().jsonPath().getObject("data", JobDTO.class);
    }

//    public <T extends EventDiscoveryOperationResultDTO> T getDiscoveryOperationResult(String orgId, String jobId, Class<T> clazz) {
//        return (T) getWithTokenAndThen(getToken(orgId), "yep", jobId)
//                .statusCode(200)
//                .contentType(ContentType.JSON)
//                .body("data.jobId", is(jobId),
//                        "data.status", notNullValue())
//                .extract().jsonPath().getObject("data", clazz);
//    }

    public String getDiscoveryOperationAsyncAPIResult(String jobId) {
        return getAndThen(APPLICATION_OPERATION_RESULT_ASYNCAPI_URL, jobId)
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().body().asString();
    }

    public JobDTO stopDiscovery(String jobId) {
        return postAndThen("{}", APPLICATION_OPERATION_STOP_URL, jobId)
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("data.id", is(jobId),
                        "data.status", notNullValue())
                .extract().jsonPath().getObject("data", JobDTO.class);
    }

    protected EventDiscoveryOperationDiscoveryResultBO getResultInInternalModel() {

        final ArgumentCaptor<EventDiscoveryOperationDiscoveryResultBO> resultArg =
                ArgumentCaptor.forClass(EventDiscoveryOperationDiscoveryResultBO.class);

        Mockito.verify(asyncApiMapper).map(resultArg.capture());

        return resultArg.getValue();
    }

    protected JobDTO waitUntilJobStatus(final JobStatus jobStatus, final String jobId) {
        // After 15 seconds, with 1 second intervals, this will fail.
        final Instant endTime = Instant.now().plus(15, ChronoUnit.SECONDS);

        while (!Duration.between(Instant.now(), endTime).isNegative()) {
            final JobDTO status = getDiscoveryOperationStatus(jobId);

            if (jobStatus.equals(status.getStatus())) {
                return status;
            }
            if (!JobStatus.RUNNING.equals(status.getStatus()) && !JobStatus.PENDING.equals(status.getStatus())) {
                assertThat("job status", status.getStatus(), is(jobStatus));
            }

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        fail("Time limit exceeded 15 seconds");
        return null;
    }
}
