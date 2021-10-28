package com.event.discovery.agent.managers;

import com.event.discovery.agent.apps.eventDiscovery.EventDiscoveryApp;
import com.event.discovery.agent.apps.model.DiscoveryApp;
import com.event.discovery.agent.apps.model.v1.DiscoveryOperationRequestBO;
import com.event.discovery.agent.apps.model.AppStopReason;
import com.event.discovery.agent.apps.model.EventDiscoveryOperationResultBO;
import com.event.discovery.agent.job.model.ApplicationJob;
import com.event.discovery.agent.job.model.JobBO;
import com.event.discovery.agent.job.model.JobContainer;
import com.event.discovery.agent.framework.exception.EntityNotFoundException;
import com.event.discovery.agent.framework.exception.ValidationException;
import com.event.discovery.agent.framework.model.AppStatus;
import com.event.discovery.agent.framework.model.JobStatus;
import com.event.discovery.agent.framework.utils.IDGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class AppsManager {

    private final JobContainer jobContainer;
    private final IDGenerator idGenerator;
    private final ObjectProvider<EventDiscoveryApp> eventDiscoveryMessageListeningAppProvider;

    @Autowired
    public AppsManager(JobContainer jobContainer, @Qualifier("discoveryIdGenerator") IDGenerator idGenerator,
                       ObjectProvider<EventDiscoveryApp> eventDiscoveryMessageListeningAppProvider) {
        this.jobContainer = jobContainer;
        this.idGenerator = idGenerator;
        this.eventDiscoveryMessageListeningAppProvider = eventDiscoveryMessageListeningAppProvider;
    }

    public String startApp(DiscoveryOperationRequestBO event) {
        return startApp(event, idGenerator.generateRandomUniqueId("1"));
    }

    public String startApp(DiscoveryOperationRequestBO event, String jobId) {
        switch (event.getDiscoveryOperation().getOperationType()) {
            case "eventDiscovery":
                return startEventDiscovery(event, jobId);
            default:
                throw new ValidationException("application", "operationType ",
                        String.format("Unknown operationType %s", event.getDiscoveryOperation().getOperationType()));
        }
    }

    private String startEventDiscovery(DiscoveryOperationRequestBO request, String jobId) {
        ApplicationJob discoveryJob = new ApplicationJob(
                jobId,
                JobStatus.RUNNING);

        EventDiscoveryApp eventDiscoveryApp = eventDiscoveryMessageListeningAppProvider.getObject();

        discoveryJob.setApp(eventDiscoveryApp);
        jobContainer.put(discoveryJob);
        eventDiscoveryApp.start(request, discoveryJob);
        return discoveryJob.getId();
    }

    public JobBO stopApp(String id) {
        ApplicationJob appJob = getJob(id);
        if (appJob.getStatus() == JobStatus.RUNNING) {
            appJob.setStatus(JobStatus.STOPPED);
            jobContainer.put(appJob);
            DiscoveryApp app = appJob.getApp();
            if (app != null) {
                app.stop(AppStopReason.MANUAL);
            }
        }
        return JobBO.builder().id(id).status(appJob.getStatus()).build();
    }

    public JobBO cancelApp(String id) {
        ApplicationJob appJob = getJob(id);
        if (appJob.getStatus() == JobStatus.RUNNING) {
            DiscoveryApp app = appJob.getApp();
            if (app != null) {
                app.stop(AppStopReason.CANCEL);
            }
        }
        appJob.setApp(null);
        appJob.setStatus(JobStatus.CANCELLED);
        jobContainer.put(appJob);
        return JobBO.builder().id(id).status(appJob.getStatus()).build();
    }

    public EventDiscoveryOperationResultBO getAppResults(String jobId) {
        ApplicationJob appJob = getJob(jobId);
        DiscoveryApp app = appJob.getApp();
        if (app != null) {
            AppStatus status = appJob.getApp().getStatus();
            if (!status.equals(AppStatus.COMPLETED) && !status.equals(AppStatus.ERROR)) {
                throw new ValidationException("applicationResult", "jobId", String.format("Job %s has not completed.", jobId));
            }
            EventDiscoveryOperationResultBO result = appJob.getApp().getResults();
            result.setStatus(status.getValue());
            if (AppStatus.ERROR == status) {
                result.setError(appJob.getApp().getError());
            }
            result.setJobId(jobId);
            return result;
        }
        // No application
        throw new EntityNotFoundException("applicationResult", "Application result for job " + jobId + " not found");
    }

    public AppStatus getAppStatus(String jobId) {
        ApplicationJob appJob = getJob(jobId);
        DiscoveryApp app = appJob.getApp();
        if (app != null) {
            return app.getStatus();
        }
        throw new EntityNotFoundException("applicationStatus", "Application status for job " + jobId + " not found");
    }

    public ApplicationJob getJob(String id) {
        Optional<ApplicationJob> appJob = jobContainer.get(id);
        if (!appJob.isPresent()) {
            throw new EntityNotFoundException("job", "Job " + id + " not found");
        }
        return appJob.get();
    }
}
