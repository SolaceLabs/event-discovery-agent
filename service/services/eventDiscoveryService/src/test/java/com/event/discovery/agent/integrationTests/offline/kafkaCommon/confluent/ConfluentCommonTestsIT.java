package com.event.discovery.agent.integrationTests.offline.kafkaCommon.confluent;

import com.event.discovery.agent.integrationTests.offline.kafkaCommon.KafkaCommonTestsIT;
import com.event.discovery.agent.kafkaConfluent.model.ConfluentSubject;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.junit.Before;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class ConfluentCommonTestsIT extends KafkaCommonTestsIT {
    @Override
    @Before
    public void before() throws ExecutionException, InterruptedException {
        super.before();
        setupConfluentSchemaRegistryMocks();
    }

    private void setupConfluentSchemaRegistryMocks() {
        when(mockWebClient.get()).thenReturn(requestHeadersUriSpec);

        // Get subject list
        setupListMockFromUri("http://schemaHost:9093/subjects", "[ \"ATopic-value\" ]");
        setupListMockFromUri("https://schemaHost:9093/subjects", "[ \"ATopic-value\" ]");

        // Get latest schema
        setupMockToGetLatestSchema("http://schemaHost:9093/subjects/ATopic-value/versions/latest");
        setupMockToGetLatestSchema("https://schemaHost:9093/subjects/ATopic-value/versions/latest");
    }

    protected void setupMocksForTwoEventsSingleSchema() {
        setupListMockFromUri("http://schemaHost:9093/subjects", "[ \"BTopic-value\", \"CTopic-value\" ]");
        setupListMockFromUri("https://schemaHost:9093/subjects", "[ \"BTopic-value\", \"CTopic-value\" ]");

        setupTopicMocks();

        // Get latest schema
        setupMockToGetLatestSchema("http://schemaHost:9093/subjects/BTopic-value/versions/latest");
        setupMockToGetLatestSchema("http://schemaHost:9093/subjects/CTopic-value/versions/latest");
        setupMockToGetLatestSchema("https://schemaHost:9093/subjects/BTopic-value/versions/latest");
        setupMockToGetLatestSchema("https://schemaHost:9093/subjects/CTopic-value/versions/latest");
    }

    private void setupMockToGetLatestSchema(String schemaUrl) {
        setupMockToGetLatestSchema(schemaUrl, 1, 0);
    }

    private void setupMockToGetLatestSchema(String schemaUrl, int version, int payloadInx) {
        List<String> payloads = new ArrayList<>();
        payloads.add("{\"type\":\"record\",\"name\":\"Payment\",\"namespace\":\"io.confluent.examples.clients.basicavro\"," +
                "\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"amount\",\"type\":\"double\"}," +
                "{\"name\":\"region\",\"type\":\"string\",\"default\":\"\"}]}");
        payloads.add("{\"type\":\"record\",\"name\":\"Payment2\",\"namespace\":\"io.confluent.examples.clients.basicavro2\"," +
                "\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"amount\",\"type\":\"double\"}," +
                "{\"name\":\"region\",\"type\":\"string\",\"default\":\"\"}]}");

        WebClient.RequestHeadersSpec requestHeadersSpecSubject = mock(WebClient.RequestHeadersSpec.class);
        doAnswer(ans -> {
            Consumer<HttpHeaders> callback = (Consumer<HttpHeaders>) ans.getArguments()[0];
            callback.accept(new HttpHeaders());
            return requestHeadersSpecSubject;
        }).when(requestHeadersSpecSubject).headers(any(Consumer.class));

        when(requestHeadersUriSpec.uri(schemaUrl))
                .thenReturn(requestHeadersSpecSubject);
        WebClient.ResponseSpec responseSpecSubject = mock(WebClient.ResponseSpec.class);
        when(requestHeadersSpecSubject.retrieve()).thenReturn(responseSpecSubject);

        Mono monoSubject = mock(Mono.class);
        when(responseSpecSubject.bodyToMono(any(Class.class))).thenReturn(monoSubject);
        ConfluentSubject subject = new ConfluentSubject();
        subject.setSchema("ATopic-value");
        subject.setVersion(version);
        subject.setId(payloadInx);
        subject.setSchema(payloads.get(payloadInx));
        when(monoSubject.block()).thenReturn(subject);
    }

    protected void setupMocksForPrimitiveKeyAndValueFromRegistry() {
        setupListMockFromUri("http://schemaHost:9093/subjects", "[ \"BTopic-value\", \"BTopic-key\" ]");
        setupListMockFromUri("https://schemaHost:9093/subjects", "[ \"BTopic-value\", \"BTopic-key\" ]");

        setupTopicMocks();

        // Get latest schema
        setupMockToGetPrimitiveSchema("http://schemaHost:9093/subjects/BTopic-value/versions/latest", 1);
        setupMockToGetPrimitiveSchema("http://schemaHost:9093/subjects/BTopic-key/versions/latest", 2);
        setupMockToGetPrimitiveSchema("https://schemaHost:9093/subjects/BTopic-value/versions/latest", 1);
        setupMockToGetPrimitiveSchema("https://schemaHost:9093/subjects/BTopic-key/versions/latest", 2);
    }

    protected void setupMocksForPrimitiveKeyAndNoValueFromRegistry() {
        setupListMockFromUri("http://schemaHost:9093/subjects", "[ \"BTopic-key\" ]");
        setupListMockFromUri("https://schemaHost:9093/subjects", "[ \"BTopic-key\" ]");

        setupTopicMocks();

        // Get latest schema
        setupMockToGetPrimitiveSchema("http://schemaHost:9093/subjects/BTopic-key/versions/latest", 3);
        setupMockToGetPrimitiveSchema("https://schemaHost:9093/subjects/BTopic-key/versions/latest", 3);
    }

    protected void setupMocksForKeyAndValueComplexSchema() {
        setupListMockFromUri("http://schemaHost:9093/subjects", "[ \"BTopic-key\", \"BTopic-value\" ]");
        setupListMockFromUri("https://schemaHost:9093/subjects", "[ \"BTopic-key\", \"BTopic-value\" ]");


        setupTopicMocks();

        // Get latest schema
        setupMockToGetLatestSchema("http://schemaHost:9093/subjects/BTopic-value/versions/latest");
        setupMockToGetLatestSchema("http://schemaHost:9093/subjects/BTopic-key/versions/latest", 5, 1);
        setupMockToGetLatestSchema("https://schemaHost:9093/subjects/BTopic-value/versions/latest");
        setupMockToGetLatestSchema("https://schemaHost:9093/subjects/BTopic-key/versions/latest", 5, 1);
    }

    protected void setupMocksForPrimitiveKeyAndComplexValueSchema() {
        setupListMockFromUri("http://schemaHost:9093/subjects", "[ \"BTopic-value\", \"BTopic-key\" ]");
        setupListMockFromUri("https://schemaHost:9093/subjects", "[ \"BTopic-value\", \"BTopic-key\" ]");


        setupTopicMocks();

        // Get latest schema
        setupMockToGetLatestSchema("http://schemaHost:9093/subjects/BTopic-value/versions/latest");
        setupMockToGetPrimitiveSchema("http://schemaHost:9093/subjects/BTopic-key/versions/latest", 6);
        setupMockToGetLatestSchema("https://schemaHost:9093/subjects/BTopic-value/versions/latest");
        setupMockToGetPrimitiveSchema("https://schemaHost:9093/subjects/BTopic-key/versions/latest", 6);
    }


    private void setupTopicMocks() {
        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        when(kafkaAdminClientMocks.getAdminClient().listTopics()).thenReturn(listTopicsResult);
        KafkaFuture<Collection<TopicListing>> mockTopicListingFuture = mock(KafkaFuture.class);
        when(listTopicsResult.listings()).thenReturn(mockTopicListingFuture);

        TopicListing topicListing1 = new TopicListing("BTopic", false);
        TopicListing topicListing2 = new TopicListing("CTopic", false);
        Collection<TopicListing> topicListings = new ArrayList<>();
        topicListings.add(topicListing1);
        topicListings.add(topicListing2);
        try {
            when(mockTopicListingFuture.get()).thenReturn(topicListings);
        } catch (InterruptedException | ExecutionException e) {
            fail();
        }
    }

    private void setupMockToGetPrimitiveSchema(String s, int schemaId) {
        WebClient.RequestHeadersSpec requestHeadersSpecSubject = mock(WebClient.RequestHeadersSpec.class);
        when(requestHeadersUriSpec.uri(s))
                .thenReturn(requestHeadersSpecSubject);
        doAnswer(ans -> {
            Consumer<HttpHeaders> callback = (Consumer<HttpHeaders>) ans.getArguments()[0];
            callback.accept(new HttpHeaders());
            return requestHeadersSpecSubject;
        }).when(requestHeadersSpecSubject).headers(any(Consumer.class));
        WebClient.ResponseSpec responseSpecSubject = mock(WebClient.ResponseSpec.class);
        when(requestHeadersSpecSubject.retrieve()).thenReturn(responseSpecSubject);
        Mono monoSubject = mock(Mono.class);
        when(responseSpecSubject.bodyToMono(any(Class.class))).thenReturn(monoSubject);
        ConfluentSubject subject = new ConfluentSubject();
        subject.setVersion(3);
        subject.setId(schemaId);
        if (schemaId == 2) {
            subject.setSchema("\"float\"");
        } else {
            subject.setSchema("\"string\"");
        }
        when(monoSubject.block()).thenReturn(subject);
    }

    protected void setupMocksForTwoEventsSingleTopic() {
        setupListMockFromUri("http://schemaHost:9093/subjects",
                "[ \"BTopic-io.confluent.examples.clients.basicavro.Payment\", \"BTopic-io.confluent.examples.clients.basicavro2.Payment2\" ]");
        setupListMockFromUri("https://schemaHost:9093/subjects",
                "[ \"BTopic-io.confluent.examples.clients.basicavro.Payment\", \"BTopic-io.confluent.examples.clients.basicavro2.Payment2\" ]");

        setupTopicMocks();

        // Get latest schema
        setupMockToGetLatestSchema("http://schemaHost:9093/subjects/BTopic-io.confluent.examples.clients.basicavro.Payment/versions/latest");
        setupMockToGetLatestSchema("http://schemaHost:9093/subjects/BTopic-io.confluent.examples.clients.basicavro2.Payment2/versions/latest", 5, 1);
        setupMockToGetLatestSchema("https://schemaHost:9093/subjects/BTopic-io.confluent.examples.clients.basicavro.Payment/versions/latest");
        setupMockToGetLatestSchema("https://schemaHost:9093/subjects/BTopic-io.confluent.examples.clients.basicavro2.Payment2/versions/latest", 5, 1);
    }

    protected void setupMocksForRecordSubjectStrategyWithComplexSchema() {
        setupListMockFromUri("http://schemaHost:9093/subjects",
                "[ \"io.confluent.examples.clients.basicavro.Payment\" ]");
        setupListMockFromUri("https://schemaHost:9093/subjects",
                "[ \"io.confluent.examples.clients.basicavro.Payment\" ]");

        setupTopicMocks();

        // Get latest schema
        setupMockToGetLatestSchema("http://schemaHost:9093/subjects/io.confluent.examples.clients.basicavro.Payment/versions/latest");
        setupMockToGetLatestSchema("https://schemaHost:9093/subjects/io.confluent.examples.clients.basicavro.Payment/versions/latest");
    }

    protected void setupMocksForUnknownTopicToConnector() {
        when(mockWebClient.get()).thenReturn(requestHeadersUriSpec);

        // Get connector List
        setupListMockFromUri("https://connectors:9095/connectors", "[ \"connector1\", \"connector2\" ]");
        setupListMockFromUri("http://connectors:9095/connectors", "[ \"connector1\", \"connector2\" ]");

        setupConnectorMockForSpecificConnector("connector1", "NoTopic", true);
        setupConnectorMockForSpecificConnector("connector2", "ATopic", false);
    }

    protected void setupMocksForUnknownTopicToConsumerGroup() throws InterruptedException, ExecutionException {
        kafkaAdminClientMocks.mockConsumerGroups(new TopicPartition("NoTopic", 0));
    }
}
