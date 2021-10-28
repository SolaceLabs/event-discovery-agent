package com.event.discovery.agent.kafkaCommon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.event.discovery.agent.types.config.ConnectTask;
import com.event.discovery.agent.types.config.KafkaConnect;
import com.event.discovery.agent.types.event.common.Client;
import com.event.discovery.agent.framework.exception.WebClientException;
import com.event.discovery.agent.framework.model.CommonModelConstants;
import com.event.discovery.agent.framework.model.ServiceAuthentication;
import com.event.discovery.agent.framework.model.ServiceIdentity;
import com.event.discovery.agent.framework.model.WebClientProperties;
import com.event.discovery.agent.framework.utils.WebUtils;
import com.event.discovery.agent.kafkaCommon.model.KafkaCommonProperties;
import com.event.discovery.agent.kafkaCommon.model.KafkaConnector;
import com.event.discovery.agent.kafkaCommon.model.KafkaConnectorTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class KafkaConnectorUtils {
    private final KafkaCommon kafkaCommon;
    private final ObjectMapper objectMapper;
    private final KafkaCommonProperties kafkaCommonProperties;
    private WebClient connectorClient;
    private final ObjectProvider<WebClient> webClientObjectProvider;
    private final Map<String, Client> connectorMap = new HashMap<>();
    public final Map<KafkaConnector.ConnectorType, String> connectorTypeToClientType = Map.of(
            KafkaConnector.ConnectorType.SOURCE, CommonModelConstants.Client.TYPE_PRODUCER,
            KafkaConnector.ConnectorType.SINK, CommonModelConstants.Client.TYPE_CONSUMER);

    public KafkaConnectorUtils(KafkaCommon kafkaCommon, ObjectMapper objectMapper, KafkaCommonProperties kafkaCommonProperties,
                               ObjectProvider<WebClient> webClientObjectProvider) {
        this.kafkaCommon = kafkaCommon;
        this.objectMapper = objectMapper;
        this.kafkaCommonProperties = kafkaCommonProperties;
        this.webClientObjectProvider = webClientObjectProvider;
    }

    public void discoveryKafkaConnectors() throws IOException {
        List<String> connectors = getConnectors();
        for (String connectorName : connectors) {
            KafkaConnector connectorConfig;
            KafkaConnector connectorStatus = null;
            try {
                connectorConfig = getConnector(connectorName, false);
            } catch (Exception ex) {
                log.warn("Could not get config for connector {}", connectorName);
                continue;
            }
            try {
                connectorStatus = getConnector(connectorName, true);
            } catch (WebClientException ex) {
                log.warn("Could not get status for connector {}", connectorName);
            }
            addConnectorStatusToConfigDiscoveryData(connectorConfig, connectorStatus);
            addConnectorClient(connectorConfig);
        }
    }

    public void createConnectorClient() {
        if (kafkaCommon.getBrokerIdentity().getConnector() != null) {
            if (kafkaCommon.getBrokerAuthentication().getConnector() != null) {
                ServiceAuthentication connector = kafkaCommon.getBrokerAuthentication().getConnector();
                WebClientProperties webClientProperties = WebUtils.buildWebClientProperties(connector);
                connectorClient = webClientObjectProvider.getObject(webClientProperties);
            } else {
                connectorClient = webClientObjectProvider.getObject(new WebClientProperties());
            }
        }
    }

    private void addConnectorStatusToConfigDiscoveryData(KafkaConnector connectorConfig,
                                                         KafkaConnector connectorStatus) {
        KafkaConnect kafkaConnect = new KafkaConnect();
        kafkaConnect.setConnectorName(connectorConfig.getName());
        kafkaConnect.setConnectorType(KafkaConnect.ConnectorType.fromValue(
                connectorConfig.getType().name().toUpperCase(Locale.getDefault())));

        // Map the tasks
        if (connectorStatus != null) {
            for (KafkaConnectorTask task : connectorStatus.getTasks()) {
                ConnectTask connectTask = new ConnectTask();
                connectTask.setTaskId(task.getTaskId());
                connectTask.setWorkerId(task.getWorkerId());
                connectTask.setTaskState(ConnectTask.TaskState.fromValue(task.getState().name()));
                kafkaConnect.getConnectTasks().add(connectTask);
            }
        }

        // Map the configuration
        kafkaConnect.getAdditionalProperties().putAll(connectorConfig.getConfig());

        List<String> listOfConnectConfigParamsThatIdentifyATopicName =
                Lists.newArrayList("kafka.topic", "topic");

        // Look for configuration that may point to a topic
        List<String> connectorConfigTopicFieldList = kafkaCommonProperties.getKafkaConnectorTopicFieldList();
        if (!CollectionUtils.isEmpty(connectorConfigTopicFieldList)) {
            listOfConnectConfigParamsThatIdentifyATopicName.addAll(connectorConfigTopicFieldList);
        }

        // Check for single topic
        for (String topicName : listOfConnectConfigParamsThatIdentifyATopicName) {
            checkForTopicNameInConnectorConfiguration(connectorConfig, kafkaConnect, topicName);
        }

        // Check for multiple topics
        if (!StringUtils.isEmpty(connectorConfig.getConfig().get("topics"))) {
            String topicListString = (String) connectorConfig.getConfig().get("topics");
            String[] topicList = topicListString.split(",");
            for (String rawTopicName : topicList) {
                String topicName = rawTopicName.trim();
                kafkaConnect.getConnectTopic().add(topicName);
                addConnectorRelationship(topicName, connectorConfig);
            }
        }

        kafkaCommon.getKafkaConfigDiscoveryData().getKafkaConnect().add(kafkaConnect);
    }

    private void addConnectorClient(KafkaConnector connectorConfig) {
        if (connectorMap.containsKey(connectorConfig.getName())) {
            return;
        }

        Client connector = new Client();
        connector.setId(kafkaCommon.getNextGeneratedId());
        connector.setName(connectorConfig.getName());

        connector.setType(connectorTypeToClientType.get(KafkaConnector.ConnectorType
                .fromValue(connectorConfig.getType().name().toUpperCase())));

        Map<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put(CommonModelConstants.Client.CLIENT_TYPE_KEY, CommonModelConstants.Client.CLIENT_TYPE_CONNECTOR);
        if (connectorConfig.getConfig().containsKey("connector.class")) {
            additionalAttributes.put(CommonModelConstants.Client.CONNECTOR_CLASS_KEY, connectorConfig.getConfig().get("connector.class"));
        }
        if (connectorConfig.getConfig().containsKey("tasks.max")) {
            additionalAttributes.put(CommonModelConstants.Client.MAX_THREADS, connectorConfig.getConfig().get("tasks.max"));
        }
        Optional.ofNullable((String) connectorConfig.getConfig().get("topics"))
                .ifPresent(topicsString -> {
                    List<String> topics = Arrays.stream(topicsString.split(",")).collect(Collectors.toList());
                    additionalAttributes.put(CommonModelConstants.Client.TOPICS, topics);
                });
        connector.setAdditionalAttributes(additionalAttributes);

        connectorMap.put(connector.getName(), connector);
        kafkaCommon.getDiscoveryData().getClients().add(connector);
    }

    private void checkForTopicNameInConnectorConfiguration(KafkaConnector connectorConfig, KafkaConnect kafkaConnect, String s) {
        if (!StringUtils.isEmpty(connectorConfig.getConfig().get(s))) {
            String topicName = (String) connectorConfig.getConfig().get(s);
            kafkaConnect.getConnectTopic().add(topicName);
            addConnectorRelationship(topicName, connectorConfig);
        }
    }

    private List<String> getConnectors() throws IOException {
        ServiceIdentity connectorId = kafkaCommon.getBrokerIdentity().getConnector();
        ServiceAuthentication connectorAuth = kafkaCommon.getBrokerAuthentication().getConnector();
        if (connectorId != null) {
            String connectorsUrl = WebUtils.getServiceUrl("/connectors", connectorId, connectorAuth);

            WebClient.RequestHeadersSpec<?> partial = connectorClient.get()
                    .uri(connectorsUrl);
            partial = WebUtils.addBasicAuthSecurity(partial, connectorAuth);
            String subjectRaw = partial
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return objectMapper.readValue(subjectRaw, new TypeReference<>() {
            });
        } else {
            return Collections.emptyList();
        }
    }

    private KafkaConnector getConnector(String connectorName, boolean status) {
        ServiceIdentity connectorId = kafkaCommon.getBrokerIdentity().getConnector();
        ServiceAuthentication connectorAuth = kafkaCommon.getBrokerAuthentication().getConnector();
        String baseUrl = "/connectors/" + connectorName;
        if (status) {
            baseUrl += "/status";
        }
        String connectorUrl = WebUtils.getServiceUrl(baseUrl, connectorId, connectorAuth);

        WebClient.RequestHeadersSpec<?> partial = connectorClient.get()
                .uri(connectorUrl);
        partial = WebUtils.addBasicAuthSecurity(partial, connectorAuth);

        return partial
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse ->
                        Mono.error(new WebClientException())
                )
                .onStatus(HttpStatus::is5xxServerError, clientResponse ->
                        Mono.error(new WebClientException())
                )
                .bodyToMono(KafkaConnector.class)
                .block();
    }

    private void addConnectorRelationship(String topicName, KafkaConnector connectorConfig) {
        if (KafkaConnector.ConnectorType.SOURCE.equals(connectorConfig.getType())) {
            addConnectorToChannelRelationship(topicName, connectorConfig);
        }
    }

    private void addConnectorToChannelRelationship(String topicName, KafkaConnector connectorConfig) {
        addConnectorClient(connectorConfig);
        Client connector = connectorMap.get(connectorConfig.getName());

        kafkaCommon.addClientToChannelRelationship(topicName, connector);
    }

    public WebClient getConnectorClient() {
        return connectorClient;
    }
}
