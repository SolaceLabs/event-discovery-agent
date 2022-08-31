package com.event.discovery.agent.apps.eventDiscovery;

import com.event.discovery.agent.apps.model.DiscoveryApp;
import com.event.discovery.agent.apps.model.v1.DiscoveryOperationRequestBO;
import com.event.discovery.agent.types.event.common.Schema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.event.discovery.agent.apps.model.AppStopReason;
import com.event.discovery.agent.apps.model.EventDiscoveryOperationDiscoveryResultBO;
import com.event.discovery.agent.apps.model.EventDiscoveryOperationResultBO;
import com.event.discovery.agent.job.model.JobContainer;
import com.event.discovery.agent.springboot.properties.DiscoveryProperties;
import com.event.discovery.agent.types.event.common.Channel;
import com.event.discovery.agent.types.event.common.ChannelToSchemaRelationship;
import com.event.discovery.agent.types.event.common.DiscoveryData;
import com.event.discovery.agent.framework.BrokerPluginFactory;
import com.event.discovery.agent.framework.VendorBrokerPlugin;
import com.event.discovery.agent.framework.model.AppStatus;
import com.event.discovery.agent.framework.model.AvroSchema;
import com.event.discovery.agent.framework.model.BrokerConfigData;
import com.event.discovery.agent.framework.model.CommonModelConstants;
import com.event.discovery.agent.framework.model.DiscoveryAppSb;
import com.event.discovery.agent.framework.model.Job;
import com.event.discovery.agent.framework.model.JobStatus;
import com.event.discovery.agent.framework.model.NormalizedEvent;
import com.event.discovery.agent.framework.model.PluginConfigData;
import com.event.discovery.agent.framework.utils.IDGenerator;
import com.event.discovery.agent.framework.utils.JsonSchemaGenerator;
import com.event.discovery.agent.kafkaCommon.model.messages.KafkaNormalizedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.event.discovery.agent.framework.model.broker.BrokerCapabilities.Capability.NO_SCHEMA_INFERENCE;

@Slf4j
public abstract class EventDiscoveryApp implements DiscoveryApp, DiscoveryAppSb {

    protected final BrokerPluginFactory brokerPluginFactory;
    protected final JobContainer jobContainer;
    protected final ObjectMapper objectMapper;
    protected final DiscoveryProperties properties;

    protected String jobId;

    protected Map<String, List<NormalizedEvent>> processedEvents = Collections.synchronizedMap(new LinkedHashMap<>());

    protected ScheduledExecutorService ses;
    protected ScheduledFuture<?> scheduledStopFuture;

    protected AppStatus status;
    protected String errorMessage;

    protected BrokerInfo brokerInfo;

    protected Map<String, Object> discoveredConfigSchema;

    private EventDiscoveryContainer eventDiscoveryContainer;

    private DiscoveryOperationRequestBO operationRequest;

    private final NormalizedMessageToEventMapper normalizedMessageToEventMapper;
    private final NormalizedEventToCommonSchemaMapper normalizedEventToCommonSchemaMapper;
    private final IDGenerator idGenerator;

    private long startTime;
    private long endTime;

    private DiscoveryData commonDiscoveryData;

    private String error;

    public EventDiscoveryApp(BrokerPluginFactory brokerPluginFactory,
                             JobContainer jobContainer,
                             JsonSchemaGenerator jsonSchemaGenerator,
                             ObjectMapper objectMapper,
                             DiscoveryProperties properties,
                             IDGenerator idGenerator) {
        this.brokerPluginFactory = brokerPluginFactory;
        this.jobContainer = jobContainer;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.idGenerator = idGenerator;

        normalizedMessageToEventMapper = new NormalizedMessageToEventMapper(this, jsonSchemaGenerator, idGenerator, objectMapper);
        normalizedEventToCommonSchemaMapper = new NormalizedEventToCommonSchemaMapper(this);
        ses = Executors.newScheduledThreadPool(1);
        ((ScheduledThreadPoolExecutor) ses).setRemoveOnCancelPolicy(true);  // Removes cancelled tasks from work queue
    }

    @Override
    public void start(DiscoveryOperationRequestBO operation, Job job) {
        startTime = System.currentTimeMillis();
        jobId = job.getId();
        status = AppStatus.RUNNING;
        operationRequest = operation;

        VendorBrokerPlugin brokerPlugin = brokerPluginFactory.getBrokerPlugin(operation.getBrokerIdentity());

        // Initialize the plugin
        brokerPlugin.initialize(operation.getBrokerIdentity(),
                operation.getBrokerAuthentication(),
                operation.getDiscoveryOperation());

        // Set the broker plugin in case we need to do cleanup
        setBrokerInfo(new BrokerInfo(brokerPlugin, null, null));

        // Get the broker capabilities
        PluginConfigData pluginConfigData = brokerPlugin.getPluginConfiguration();

        BrokerConfigData brokerConfig = brokerPlugin.getBrokerConfig();
        setBrokerInfo(new BrokerInfo(brokerPlugin, pluginConfigData, brokerConfig.getConfiguration()));

        if (brokerConfig.getCommonDiscoveryData() == null) {
            commonDiscoveryData = new DiscoveryData();
        } else {
            commonDiscoveryData = brokerConfig.getCommonDiscoveryData();
        }
    }

    @Override
    public void stop(AppStopReason stopReason) {
        log.debug("Stopping app for reason {}", stopReason);
        stopApplication();

        if (!scheduledStopFuture.isDone()) {
            scheduledStopFuture.cancel(true);
        }

        if (!ses.isShutdown()) {
            ses.shutdownNow();
        }

        completeEventProcessing();
    }

    public abstract void stopApplication();

    public void mapNormalizedMessageToNormalizedEvent(NormalizedEvent normalizedEvent) {
        normalizedMessageToEventMapper.mapNormalizedMessageToNormalizedEvent(normalizedEvent);
    }

    private Map<String, Schema>
    mapNormalizedEventToCommonSchemas(NormalizedEvent event) {
        return normalizedEventToCommonSchemaMapper.mapNormalizedEventToSchemas(event);
    }

    protected void completeEventProcessing() {

        log.debug("Got {} processed events", processedEvents.keySet().size());

        mapDiscoveredTopicsToChannels();

        if (!noSchemaInference()) {
            // Build the schema list (map normalized events into schemas)
            mapEventsToChannelsAndRelationships();
        }

        jobContainer.get(jobId).ifPresent(job -> job.setStatus(JobStatus.COMPLETED));
        status = AppStatus.COMPLETED;
        endTime = System.currentTimeMillis();
        logDiscoveryData(jobId);
    }

    private void mapEventsToChannelsAndRelationships() {
        Map<String, Channel> chanelNameToChannel = commonDiscoveryData.getChannels().stream()
                .filter(channel -> CommonModelConstants.Channel.TOPIC
                        .equals(channel.getAdditionalAttributes()
                                .get(CommonModelConstants.Channel.TYPE)))
                 .collect(Collectors.toMap(Channel::getName, Function.identity(), (a, b) -> {
                     System.out.println("duplicate key found!");
                     return a;
                }
        ));
//                .collect(Collectors.toMap(Channel::getName, Function.identity()));

        Map<String, Schema> schemaMap = new HashMap<>();

        processedEvents.keySet().
                forEach(eventKey ->
                        processedEvents.get(eventKey).forEach(event -> {
                            Map<String, Schema> schemas =
                                    mapNormalizedEventToCommonSchemas(event);
                            schemas.forEach((key, value) -> schemaMap.computeIfAbsent(key, k -> {
                                commonDiscoveryData.getSchemas().add(value);
                                return value;
                            }));

                            if (StringUtils.isNotEmpty(event.getChannelName())) {
                                Channel channel = chanelNameToChannel.get(event.getChannelName());
                                Schema valueSchema =
                                        schemaMap.get(event.getSchemaId());

                                if (channel != null && valueSchema != null) {
                                    ChannelToSchemaRelationship channelToSchemaRelationship = new ChannelToSchemaRelationship();
                                    channelToSchemaRelationship.setChannelId(channel.getId());
                                    channelToSchemaRelationship.setSchemaId(valueSchema.getId());
                                    Optional.ofNullable(schemaMap.get(getKeySchemaId(event)))
                                            .ifPresent(keySchema -> channelToSchemaRelationship.setAdditionalAttributes(Map.of(
                                                    CommonModelConstants.ChannelToSchemaRelationship.KEY_SCHEMA_ID, keySchema.getId()
                                            )));
                                    commonDiscoveryData.getChannelToSchemaRelationships().add(channelToSchemaRelationship);
                                }
                            }
                        }));
    }

    private String getKeySchemaId(NormalizedEvent event) {
        if (!(event instanceof KafkaNormalizedEvent)) {
            return null;
        }

        return ((KafkaNormalizedEvent) event).getKeySchemaId();
    }

    private void mapDiscoveredTopicsToChannels() {

        // Keep a list of channel names discovered in configuration to de-dup the
        // dynamic topics discovered during message listening
        List<String> existingChannelNames = commonDiscoveryData.getChannels().stream()
                .map(Channel::getName)
                .collect(Collectors.toList());

        processedEvents.values()
                .forEach(eList -> eList
                        .forEach(e -> {
                            if ("topic".equals(e.getChannelType())) {
                                if (StringUtils.isNotEmpty(e.getChannelName())
                                        && !existingChannelNames.contains(e.getChannelName())) {
                                    Channel channel = new Channel();
                                    channel.setId(idGenerator.generateRandomUniqueId("1"));
                                    channel.setName(e.getChannelName());
                                    channel.setAdditionalAttributes(new HashMap<>());
                                    channel.getAdditionalAttributes().put("type", e.getChannelType());
                                    if (commonDiscoveryData.getChannels() == null) {
                                        commonDiscoveryData.setChannels(new ArrayList<>());
                                    }
                                    commonDiscoveryData.getChannels().add(channel);
                                }
                            }
                        }));
    }

    @Override
    public EventDiscoveryOperationResultBO getResults() {

        // Add the discovered events
        EventDiscoveryOperationDiscoveryResultBO discoveryResult = new EventDiscoveryOperationDiscoveryResultBO();

        // Add the config data
        if (properties.isIncludeConfiguration()) {
            if (getBrokerInfo().getDiscoveredConfig() != null) {
                discoveryResult.setConfiguration(getBrokerInfo().getDiscoveredConfig());
            }
        }

        discoveryResult.setJobId(jobId);

        discoveryResult.setDiscoveryStartTime(startTime);
        discoveryResult.setDiscoveryEndTime(endTime);

        Map<String, Object> operationRequestMap = getJavaComplexObjectAsMapStringObject(operationRequest);
        discoveryResult.setName(operationRequest.getDiscoveryOperation().getName());
        discoveryResult.setPluginInputs(operationRequestMap);

        discoveryResult.setBrokerType(getBrokerType(operationRequest.getBrokerIdentity().getBrokerType()));
        discoveryResult.setPluginType(operationRequest.getBrokerIdentity().getBrokerType().toLowerCase());
        discoveryResult.setDiscoverySchemaVersion("0.0.2");
        discoveryResult.setEvents(commonDiscoveryData);
        discoveryResult.setDiscoveryAgentVersion(properties.getVersion().toString());
        discoveryResult.setWarnings(Collections.emptyList());
        discoveryResult.setError(error);

        return discoveryResult;
    }

    private void logDiscoveryData(final String jobId) {
        StringBuilder commonDiscoveryResultMessage = new StringBuilder();
        // entities
        if (commonDiscoveryData.getChannels().size() > 0) {
            addToDiscoveryDataLog(commonDiscoveryResultMessage, commonDiscoveryData.getChannels().size() + " channel(s)");
        }
        if (commonDiscoveryData.getClients().size() > 0) {
            addToDiscoveryDataLog(commonDiscoveryResultMessage, commonDiscoveryData.getClients().size() + " client(s)");
        }
        if (commonDiscoveryData.getSchemas().size() > 0) {
            addToDiscoveryDataLog(commonDiscoveryResultMessage, commonDiscoveryData.getSchemas().size() + " schema(s)");
        }
        if (commonDiscoveryData.getSubscriptions().size() > 0) {
            addToDiscoveryDataLog(commonDiscoveryResultMessage, commonDiscoveryData.getSubscriptions().size() + " subscription(s)");
        }

        // relationships
        if (commonDiscoveryData.getChannelToChannelRelationships().size() > 0) {
            addToDiscoveryDataLog(commonDiscoveryResultMessage,
                    commonDiscoveryData.getChannelToChannelRelationships().size() + " channel to channel relationship(s)");
        }
        if (commonDiscoveryData.getChannelToSchemaRelationships().size() > 0) {
            addToDiscoveryDataLog(commonDiscoveryResultMessage,
                    commonDiscoveryData.getChannelToSchemaRelationships().size() + " channel to schema relationship(s)");
        }
        if (commonDiscoveryData.getChannelToSubscriptionRelationships().size() > 0) {
            addToDiscoveryDataLog(commonDiscoveryResultMessage,
                    commonDiscoveryData.getChannelToSubscriptionRelationships().size() + " channel to subscription relationship(s)");
        }
        if (commonDiscoveryData.getClientToChannelRelationships().size() > 0) {
            addToDiscoveryDataLog(commonDiscoveryResultMessage,
                    commonDiscoveryData.getClientToChannelRelationships().size() + " client to channel relationship(s)");
        }
        if (commonDiscoveryData.getClientToSubscriptionRelationships().size() > 0) {
            addToDiscoveryDataLog(commonDiscoveryResultMessage,
                    commonDiscoveryData.getClientToSubscriptionRelationships().size() + " client to subscription relationship(s)");
        }

        log.info("Found the following entities for job id " + jobId + ": " + commonDiscoveryResultMessage.toString());
    }

    private void addToDiscoveryDataLog(final StringBuilder stringBuilder, final String msg) {
        if (stringBuilder.length() > 0) {
            stringBuilder.append(", ");
        }
        stringBuilder.append(msg);
    }

    public String getSchemaNameFromAvroSchemaAndVersion(String schemaContent, String version, String topicName, boolean key) {
        try {
            AvroSchema avroSchema = objectMapper.readValue(schemaContent, AvroSchema.class);
            if (version != null) {
                return getSchemaNameFromAvroSchemaContent(avroSchema, topicName) + "." + version;
            }
            return getSchemaNameFromAvroSchemaContent(avroSchema, topicName);
        } catch (JsonProcessingException e) {
            log.warn("AVRO schema deserialization failed for schema {}. Falling back to using topicName for schema",
                    schemaContent);
            return key ? topicName + "-key" : topicName;
        }
    }

    private String getSchemaNameFromAvroSchemaContent(AvroSchema avroSchema, String topicName) {
        if (avroSchema.getNamespace() == null && avroSchema.getName() == null ||
                avroSchema.getName() == null) {
            return topicName;
        } else if (avroSchema.getNamespace() == null) {
            return topicName + "." + avroSchema.getName();
        }
        return avroSchema.getNamespace() + "." + avroSchema.getName();
    }

    private String getBrokerType(String brokerType) {
        if ("confluent".equalsIgnoreCase(brokerType)) {
            return "kafka";
        }
        return brokerType.toLowerCase();
    }

    private Map<String, Object> getJavaComplexObjectAsMapStringObject(Object complexObject) {
        return objectMapper.convertValue(complexObject, Map.class);
    }

    @Override
    public AppStatus getStatus() {
        return status;
    }

    public boolean noSchemaInference() {
        if (getBrokerInfo() != null && getBrokerInfo().getBrokerPlugin() != null) {
            return getBrokerInfo().getBrokerPlugin().hasCapability(NO_SCHEMA_INFERENCE);
        }
        return false;
    }


    // Helper methods for managing the processed event map.
    // The processedEvents map uses topic name as the key
    public boolean eventInList(NormalizedEvent normalizedEvent) {
        // If event has a schema always add it
        if (!StringUtils.isEmpty(normalizedEvent.getSchema())) {
            return false;
        }
        return processedEvents.containsKey(normalizedEvent.getChannelName());
    }

    public void addEventToList(NormalizedEvent normalizedEvent) {
        if (!processedEvents.containsKey(normalizedEvent.getChannelName())) {
            processedEvents.put(normalizedEvent.getChannelName(), new ArrayList<>());
        }
        processedEvents.get(normalizedEvent.getChannelName()).add(normalizedEvent);
    }


    @Override
    public void updateAppStatus(final AppStatus appStatus) {
        Objects.requireNonNull(appStatus);
        status = appStatus;
    }

    public BrokerPluginFactory getBrokerPluginFactory() {
        return brokerPluginFactory;
    }

    public void setJobId(final String jobId) {
        this.jobId = jobId;
    }

    public void setBrokerInfo(final BrokerInfo brokerInfo) {
        this.brokerInfo = brokerInfo;
    }

    public BrokerInfo getBrokerInfo() {
        return brokerInfo;
    }

    protected void setEventDiscoveryContainer(final EventDiscoveryContainer eventDiscoveryContainer) {
        this.eventDiscoveryContainer = eventDiscoveryContainer;
    }

    public String getNextGeneratedId() {
        return idGenerator.generateRandomUniqueId("1");
    }

    protected EventDiscoveryContainer getEventDiscoveryContainer() {
        return eventDiscoveryContainer;
    }

    @Override
    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String getError() {
        return error;
    }
}
