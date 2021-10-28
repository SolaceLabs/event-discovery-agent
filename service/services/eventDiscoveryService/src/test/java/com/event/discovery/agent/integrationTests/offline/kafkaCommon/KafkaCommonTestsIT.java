package com.event.discovery.agent.integrationTests.offline.kafkaCommon;

import com.event.discovery.agent.integrationTests.offline.BaseEventDiscoveryAgentOfflineAnnotatedIT;
import com.event.discovery.agent.integrationTests.offline.kafkaCommon.confluent.KafkaResultValidation;
import com.event.discovery.agent.integrationTests.offline.kafkaCommon.kafka.KafkaConfigRequests;
import com.event.discovery.agent.kafkaCommon.model.KafkaConnector;
import com.event.discovery.agent.kafkaCommon.model.KafkaConnectorTask;
import com.event.discovery.agent.types.event.common.Client;
import com.event.discovery.agent.types.event.common.DiscoveryData;
import com.event.discovery.agent.framework.model.CommonModelConstants;
import org.apache.commons.codec.Charsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.record.TimestampType;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public abstract class KafkaCommonTestsIT extends BaseEventDiscoveryAgentOfflineAnnotatedIT {
    @Autowired
    protected KafkaConsumer kafkaConsumer;

    @Autowired
    protected WebClient mockWebClient;

    @Autowired
    protected KafkaAdminClientMocks kafkaAdminClientMocks;

    // Used for web apis
    protected WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);

    protected KafkaConfigRequests kafkaConfigRequests = new KafkaConfigRequests();

    protected KafkaResultValidation kafkaResultValidation;

    @Before
    public void before() throws ExecutionException, InterruptedException {
        kafkaResultValidation = new KafkaResultValidation(discoveryProperties);
        resetMocks();
        kafkaAdminClientMocks.setupAdminClientMock();
        kafkaAdminClientMocks.setupKafkaAdminClient();
        kafkaAdminClientMocks.setUpDefaultAcls();
        setupKafkaDefaultRecords();
        setupKafkaConnectorMocks();
    }


    private void resetMocks() {
        kafkaAdminClientMocks.resetAdminClientMock();
        reset(kafkaConsumer);
        reset(mockWebClient);
    }

    private void setupKafkaConnectorMocks() {
        when(mockWebClient.get()).thenReturn(requestHeadersUriSpec);

        // Get connector List
        setupListMockFromUri("https://connectors:9095/connectors", "[ \"connector1\", \"connector2\" ]");
        setupListMockFromUri("http://connectors:9095/connectors", "[ \"connector1\", \"connector2\" ]");

        setupConnectorMockForSpecificConnector("connector1", "ATopic", true);
        setupConnectorMockForSpecificConnector("connector2", "ATopic", false);
    }

    protected void setupConnectorMockForSpecificConnector(String connectorName, String topicName, boolean source) {
        WebClient.RequestHeadersSpec requestHeadersSpecSubject = mock(WebClient.RequestHeadersSpec.class);

        doAnswer(ans -> {
            Consumer<HttpHeaders> callback = (Consumer<HttpHeaders>) ans.getArguments()[0];
            callback.accept(new HttpHeaders());
            return requestHeadersSpecSubject;
        }).when(requestHeadersSpecSubject).headers(any(Consumer.class));

        when(requestHeadersUriSpec.uri("https://connectors:9095/connectors/" + connectorName + "/status"))
                .thenReturn(requestHeadersSpecSubject);
        when(requestHeadersUriSpec.uri("https://connectors:9095/connectors/" + connectorName))
                .thenReturn(requestHeadersSpecSubject);
        when(requestHeadersUriSpec.uri("http://connectors:9095/connectors/" + connectorName + "/status"))
                .thenReturn(requestHeadersSpecSubject);
        when(requestHeadersUriSpec.uri("http://connectors:9095/connectors/" + connectorName))
                .thenReturn(requestHeadersSpecSubject);
        WebClient.ResponseSpec responseSpecSubject = mock(WebClient.ResponseSpec.class);
        when(requestHeadersSpecSubject.retrieve()).thenReturn(responseSpecSubject);
        when(responseSpecSubject.onStatus(any(), any())).thenReturn(responseSpecSubject);
        Mono monoSubject = mock(Mono.class);
        when(responseSpecSubject.bodyToMono(any(Class.class))).thenReturn(monoSubject);
        KafkaConnector kafkaConnector = new KafkaConnector();
        kafkaConnector.setName(connectorName);
        KafkaConnectorTask kafkaConnectorTask = new KafkaConnectorTask();
        kafkaConnectorTask.setState(KafkaConnectorTask.TaskState.UNASSIGNED);
        kafkaConnectorTask.setTaskId(3);
        kafkaConnectorTask.setWorkerId("4");
        kafkaConnector.setTasks(Collections.singletonList(kafkaConnectorTask));
        kafkaConnector.setConfig(new HashMap<>());
        if (source) {
            kafkaConnector.getConfig().put("kafka.topic", topicName);
            kafkaConnector.setType(KafkaConnector.ConnectorType.SOURCE);
        } else {
            kafkaConnector.getConfig().put("topics", topicName);
            kafkaConnector.setType(KafkaConnector.ConnectorType.SINK);
        }
        kafkaConnector.getConfig().put("connector.class", "VeryBestConnector");
        kafkaConnector.getConfig().put("tasks.max", "1");
        when(monoSubject.block()).thenReturn(kafkaConnector);
    }

    protected void setupListMockFromUri(String uri, String listJson) {
        WebClient.RequestHeadersSpec requestHeadersSpecSubjectList = mock(WebClient.RequestHeadersSpec.class);
        when(requestHeadersUriSpec.uri(uri)).thenReturn(requestHeadersSpecSubjectList);
        doAnswer(ans -> {
            Consumer<HttpHeaders> callback = (Consumer<HttpHeaders>) ans.getArguments()[0];
            callback.accept(new HttpHeaders());
            return requestHeadersSpecSubjectList;
        }).when(requestHeadersSpecSubjectList).headers(any(Consumer.class));

        WebClient.ResponseSpec responseSpecSubjectList = mock(WebClient.ResponseSpec.class);
        when(requestHeadersSpecSubjectList.retrieve()).thenReturn(responseSpecSubjectList);
        when(responseSpecSubjectList.onStatus(any(), any())).thenReturn(responseSpecSubjectList);
        Mono monoSubjectList = mock(Mono.class);
        when(responseSpecSubjectList.bodyToMono(any(Class.class))).thenReturn(monoSubjectList);
        when(monoSubjectList.block()).thenReturn(listJson);
    }

    private void setupKafkaDefaultRecords() {
        // Create an empty record
        TopicPartition topicPartition = new TopicPartition("ATopic", 0);
        Map<TopicPartition, List<ConsumerRecord<byte[], byte[]>>> emptyRecord = createConsumerRecordMap(topicPartition,
                Collections.emptyList());

        // Create a "real" record
        String payload = "{\"testattribute\": \"testvalue\", \"ted\" : [], \"bill\" : []}";
        ConsumerRecord<byte[], byte[]> realRecord1 = new ConsumerRecord<>("ATopic", 0, 1, System.currentTimeMillis(),
                TimestampType.CREATE_TIME, 0, 0, payload.length(), null, payload.getBytes(Charsets.UTF_8));
        ConsumerRecord<byte[], byte[]> realRecord2 = new ConsumerRecord<>("BTopic", 0, 1, System.currentTimeMillis(),
                TimestampType.CREATE_TIME, 0, 0, payload.length(), null, payload.getBytes(Charsets.UTF_8));
        Map<TopicPartition, List<ConsumerRecord<byte[], byte[]>>> realRecords = createConsumerRecordMap(topicPartition,
                Arrays.asList(realRecord1, realRecord2));

        Map<TopicPartition, Long> partitionOffsets = new HashMap<>();
        partitionOffsets.put(topicPartition, 0L);
        when(kafkaConsumer.endOffsets(any(Collection.class))).thenReturn(partitionOffsets);

        // Setup messaging
        when(kafkaConsumer.poll(any(Duration.class)))
                .thenReturn(new ConsumerRecords<>(emptyRecord), new ConsumerRecords<>(realRecords));
    }


    protected Map<TopicPartition, List<ConsumerRecord<byte[], byte[]>>> createConsumerRecordMap(TopicPartition topicPartition,
                                                                                                List<ConsumerRecord<byte[], byte[]>> records) {
        Map<TopicPartition, List<ConsumerRecord<byte[], byte[]>>> emptyRecord = new HashMap<>();
        emptyRecord.put(topicPartition, records);
        return emptyRecord;
    }

    protected void consumeRecords(String topic, String key, String value) {
        TopicPartition topicPartition = new TopicPartition(topic, 0);
        ConsumerRecord<byte[], byte[]> realRecord = new ConsumerRecord("ATopic", 0, 1, System.currentTimeMillis(),
                TimestampType.CREATE_TIME, 0, key.length(), value.length(), key.getBytes(Charsets.UTF_8),
                value.getBytes(Charsets.UTF_8));
        Map<TopicPartition, List<ConsumerRecord<byte[], byte[]>>> realRecords = createConsumerRecordMap(topicPartition, Collections.singletonList(realRecord));
        when(kafkaConsumer.poll(any(Duration.class))).thenReturn(new ConsumerRecords(realRecords));
    }

    protected List<Client> getConnectors(final DiscoveryData discoveryData) {
        if (discoveryData.getClients() == null) {
            return Collections.emptyList();
        }

        return discoveryData.getClients().stream()
                .filter(this::isConnector)
                .collect(Collectors.toList());
    }

    protected List<Client> getConsumerGroups(final DiscoveryData discoveryData) {
        if (discoveryData.getClients() == null) {
            return Collections.emptyList();
        }

        return discoveryData.getClients().stream()
                .filter(this::isConsumerGroup)
                .collect(Collectors.toList());
    }

    protected boolean isConsumerGroup(final Client client) {
        if (client == null) {
            return false;
        }

        return !isConnector(client);
    }

    protected boolean isConnector(final Client client) {
        if (client == null) {
            return false;
        }

        var attrs = client.getAdditionalAttributes();
        if (attrs == null) {
            return false;
        }

        return Objects.nonNull(attrs.get(CommonModelConstants.Client.CONNECTOR_CLASS_KEY));
    }
}
