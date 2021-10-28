package com.event.discovery.agent.apps.eventDiscovery;

import com.event.discovery.agent.apps.model.DiscoveryApp;
import com.event.discovery.agent.apps.model.v1.DiscoveryOperationRequestBO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.event.discovery.agent.job.model.JobContainer;
import com.event.discovery.agent.springboot.properties.DiscoveryProperties;
import com.event.discovery.agent.task.JobExecutorService;
import com.event.discovery.agent.task.TaskRunner;
import com.event.discovery.agent.task.realtime.AsynchronousMessageConsumer;
import com.event.discovery.agent.task.realtime.AsynchronousMessageProcessor;
import com.event.discovery.agent.task.realtime.BrokerConsumer;
import com.event.discovery.agent.task.realtime.BrokerNormalizedEventProcessor;
import com.event.discovery.agent.task.realtime.SynchronousMessageConsumer;
import com.event.discovery.agent.task.realtime.SynchronousMessageProcessor;
import com.event.discovery.agent.framework.BrokerPluginFactory;
import com.event.discovery.agent.framework.model.DiscoveryAppSb;
import com.event.discovery.agent.framework.model.Job;
import com.event.discovery.agent.framework.model.NormalizedEvent;
import com.event.discovery.agent.framework.model.broker.BrokerCapabilities;
import com.event.discovery.agent.framework.utils.IDGenerator;
import com.event.discovery.agent.framework.utils.JsonSchemaGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RealtimeEventDiscoveryApp extends EventDiscoveryApp implements DiscoveryApp, DiscoveryAppSb {
    private final JobExecutorService jobExecutorService;
    private final TaskRunner taskRunner;

    @Autowired
    public RealtimeEventDiscoveryApp(BrokerPluginFactory brokerPluginFactory,
                                     JobContainer jobContainer,
                                     JsonSchemaGenerator jsonSchemaGenerator,
                                     ObjectMapper objectMapper,
                                     JobExecutorService jobExecutorService,
                                     DiscoveryProperties discoveryProperties,
                                     IDGenerator idGenerator) {
        super(brokerPluginFactory, jobContainer, jsonSchemaGenerator, objectMapper,
                discoveryProperties, idGenerator);
        this.jobExecutorService = jobExecutorService;
        taskRunner = TaskRunner.create();
    }

    @Override
    public void start(DiscoveryOperationRequestBO operation, Job job) {
        super.setEventDiscoveryContainer(new EventDiscoveryContainer(job, this, operation));

        getJobExecutorService().execute(this, job, () -> {
            try {
                super.start(operation, job);

                if (isSchemaRegistryPresent()) {
                    new BrokerNormalizedEventProcessor(getEventDiscoveryContainer()).start();
                    completeEventProcessing();
                } else {
                    final LinkedBlockingQueue<NormalizedEvent> normalizedMessageQueue =
                            new LinkedBlockingQueue<>(100000);
                    final BrokerConsumer brokerConsumer = new BrokerConsumer(getEventDiscoveryContainer());
                    try {
                        brokerConsumer.start();
                        if (isTimedMessageConsumer()) {
                            activeTimedMessageListener(operation, job, normalizedMessageQueue);
                        } else {
                            new SynchronousMessageConsumer(getEventDiscoveryContainer(), normalizedMessageQueue).start();
                            new SynchronousMessageProcessor(getEventDiscoveryContainer(), normalizedMessageQueue).start();
                        }
                        completeEventProcessing();
                    } finally {
                        brokerConsumer.stop();
                    }
                }
            } finally {
                if (getBrokerInfo() != null && getBrokerInfo().getBrokerPlugin() != null) {
                    getBrokerInfo().getBrokerPlugin().closeSession();
                }
            }
        });
    }

    private void activeTimedMessageListener(DiscoveryOperationRequestBO operation, Job job, LinkedBlockingQueue<NormalizedEvent> normalizedMessageQueue) {
        AsynchronousMessageConsumer consumer =
                new AsynchronousMessageConsumer(getEventDiscoveryContainer(), normalizedMessageQueue);
        AsynchronousMessageProcessor processor =
                new AsynchronousMessageProcessor(getEventDiscoveryContainer(), normalizedMessageQueue);
        try {
            getJobExecutorService().execute(this, job, consumer);
            getJobExecutorService().execute(this, job, processor);
            // Wait for the messages to be collected
            wait(operation.getDiscoveryOperation().getDurationInSecs());
        } finally {
            consumer.stop();
            // Wait for remaining messages to be processed
            wait(1);
            processor.stop();
        }
    }

    private void wait(int waitTimeInSeconds) {
        try {
            TimeUnit.SECONDS.sleep(waitTimeInSeconds);
        } catch (InterruptedException e) {
            log.error("Could not sleep thread", e);
        }
    }

    @Override
    public void stopApplication() {
        taskRunner.stop();
    }

    private JobExecutorService getJobExecutorService() {
        return jobExecutorService;
    }

    private boolean isSchemaRegistryPresent() {
        if (getBrokerInfo().getPluginConfigData() != null && getBrokerInfo().getPluginConfigData().getBrokerCapabilities() != null) {
            return getBrokerInfo().getPluginConfigData().getBrokerCapabilities().supports(BrokerCapabilities.Capability.SCHEMA_REGISTRY);
        }
        return true;
    }

    private boolean isTimedMessageConsumer() {
        if (getBrokerInfo().getPluginConfigData() != null && getBrokerInfo().getPluginConfigData().getBrokerCapabilities() != null) {
            return getBrokerInfo().getPluginConfigData().getBrokerCapabilities().supports(BrokerCapabilities.Capability.TIMED_MESSAGE_CONSUMER);
        }
        return true;
    }

}
