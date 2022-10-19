package com.event.discovery.agent.kafkaConfluent;

import com.event.discovery.agent.kafkaConfluent.model.ConfluentSubject;
import com.event.discovery.agent.kafkaConfluent.model.SubjectNameStrategy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.event.discovery.agent.framework.SchemaEvents;
import com.event.discovery.agent.framework.exception.DiscoverySupportCode;
import com.event.discovery.agent.framework.exception.EventDiscoveryAgentException;
import com.event.discovery.agent.framework.model.AvroSchema;
import com.event.discovery.agent.framework.model.BasicPluginConfigData;
import com.event.discovery.agent.framework.model.BrokerConfigData;
import com.event.discovery.agent.framework.model.DiscoveryOperation;
import com.event.discovery.agent.framework.model.NormalizedEvent;
import com.event.discovery.agent.framework.model.PluginConfigData;
import com.event.discovery.agent.framework.model.PluginObjectMapper;
import com.event.discovery.agent.framework.model.SchemaType;
import com.event.discovery.agent.framework.model.ServiceAuthentication;
import com.event.discovery.agent.framework.model.ServiceIdentity;
import com.event.discovery.agent.framework.model.WebClientProperties;
import com.event.discovery.agent.framework.model.broker.BrokerAuthentication;
import com.event.discovery.agent.framework.model.broker.BrokerCapabilities;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;
import com.event.discovery.agent.framework.utils.WebUtils;
import com.event.discovery.agent.kafkaCommon.KafkaCommon;
import com.event.discovery.agent.kafkaCommon.model.messages.KafkaNormalizedEventImpl;
import com.event.discovery.agent.kafkaCommon.model.messages.KafkaNormalizedMessageImpl;
import com.event.discovery.agent.kafkaCommon.plugin.AbstractApacheKafkaPlugin;
import com.event.discovery.agent.kafkaConfluent.model.ConfluentBrokerAuthentication;
import com.event.discovery.agent.kafkaConfluent.model.ConfluentKafkaIdentity;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.ssl.SSLException;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
public class ConfluentKafkaPlugin extends AbstractApacheKafkaPlugin implements SchemaEvents {
    private WebClient webClient;
    private final ObjectProvider<WebClient> webClientObjectProvider;
    private final ObjectMapper objectMapper;

    private Map<String, NormalizedEvent> topicToNormalizedEventMap = new HashMap<>();

    private final static String TOPIC_SUBJECT_VALUE_SUFFIX = "-value";
    private final static String TOPIC_SUBJECT_KEY_SUFFIX = "-key";

    @Autowired
    public ConfluentKafkaPlugin(ObjectProvider<KafkaConsumer<byte[], byte[]>> kafkaConsumerObjectProvider,
                                MeterRegistry meterRegistry, KafkaCommon kafkaCommon,
                                ObjectProvider<WebClient> webClientObjectProvider,
                                PluginObjectMapper pluginObjectMapper) {
        super(kafkaConsumerObjectProvider, meterRegistry, kafkaCommon);
        objectMapper = pluginObjectMapper.getObjectMapper();
        this.webClientObjectProvider = webClientObjectProvider;
    }

    @Override
    public void initialize(BrokerIdentity brokerIdentity, BrokerAuthentication brokerAuthentication,
                           DiscoveryOperation discoveryOperation) {
        super.initialize(brokerIdentity, brokerAuthentication, discoveryOperation);
        ConfluentBrokerAuthentication confluentBrokerAuthentication = (ConfluentBrokerAuthentication) brokerAuthentication;
        if (        confluentBrokerAuthentication.getSchemaRegistry() != null
                &&  confluentBrokerAuthentication.getSchemaRegistry().getTrustStoreLocation() != null
                &&  confluentBrokerAuthentication.getSslNoVerify() != null
                &&  confluentBrokerAuthentication.getSslNoVerify() == Boolean.TRUE) {
            try {
                log.warn("sslNoVerify requested; using InsecureTrustManagerFactory for schema registry connection");
                SslContext sslContext = SslContextBuilder
                        .forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
                HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
                webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
            } catch ( SSLException sslexc ) {
                log.error("Creation of Insecure SSL connection failed with mesage: %s", sslexc.getMessage());
            }
        } else {
            if (confluentBrokerAuthentication.getSchemaRegistry() != null) {
                ServiceAuthentication schemaRegistry = confluentBrokerAuthentication.getSchemaRegistry();
                WebClientProperties webClientProperties = WebUtils.buildWebClientProperties(schemaRegistry);
                webClient = webClientObjectProvider.getObject(webClientProperties);
            } else {
                webClient = webClientObjectProvider.getObject(new WebClientProperties());
            }
        }
    }

    @Override
    public BrokerConfigData getBrokerConfig() {
        BrokerConfigData discoveredConfig = super.getBrokerConfig();

        // Discover Schemas, determine topic, lookup consumer group subscribers and
        // create a normalized event
        try {
            createNormalizedEventsFromSchemas();
        } catch (IOException e) {
            log.error("Could not get subjects", e);
            throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101,
                    "Error getting schemas from schema registry", e);
        }
        return discoveredConfig;
    }

    @Override
    protected String getPluginName() {
        return "CONFLUENT";
    }

    private void createNormalizedEventsFromSchemas() throws IOException {
        List<String> subjects = getSubjects();

        for (String subject : subjects) {
            if (subject.endsWith(TOPIC_SUBJECT_VALUE_SUFFIX) || subject.endsWith(TOPIC_SUBJECT_KEY_SUFFIX)) {
                // This is topic name strategy for the subject
                String topic = getTopicFromSubject(subject);
                if (kafkaCommon.getTopics().contains(topic) && kafkaCommon.topicNameMatchesSubscriptionSet(topic)) {
                    log.debug("Found topic {} from subject {}", topic, subject);
                    ConfluentSubject subjectResult = getLatestSchemaForSubject(subject);
                    log.debug("Found schema {}  version {}", subjectResult.getSchema(), subjectResult.getVersion());
                    normalizeEventAndStoreInEventMap(SubjectNameStrategy.TOPIC, subject, topic, subjectResult);
                }
            } else {
                // Assume for now that is topic-record strategy for the subject
                // Get the schema so we can figure out the name and namespace
                ConfluentSubject subjectResult = getLatestSchemaForSubject(subject);
                AvroSchema avroSchema = objectMapper.readValue(subjectResult.getSchema(), AvroSchema.class);
                String name = avroSchema.getName();
                String namespace = avroSchema.getNamespace();
                String expectedSubjectSuffix = "-" + namespace + "." + name;
                if (subject.endsWith(expectedSubjectSuffix)) {
                    String topicName = StringUtils.removeEnd(subject, expectedSubjectSuffix);
                    normalizeEventAndStoreInEventMap(SubjectNameStrategy.TOPIC_RECORD,
                            subject, topicName, subjectResult);
                } else {
                    // Assume its Record name subject strategy since this is the last remaining option
                    normalizeEventAndStoreInEventMap(SubjectNameStrategy.RECORD,
                            subject, null, subjectResult);
                }
            }
        }
    }

    private List<String> getSubjects() throws IOException {
        ServiceIdentity schemaRegId = ((ConfluentKafkaIdentity) getBrokerIdentity()).getSchemaRegistry();
        ServiceAuthentication schemaRegAuth = ((ConfluentBrokerAuthentication) getBrokerAuthentication()).getSchemaRegistry();
        String schemaUrl = WebUtils.getServiceUrl("/subjects", schemaRegId, schemaRegAuth);
        ConfluentBrokerAuthentication confluentBrokerAuthentication = (ConfluentBrokerAuthentication) getBrokerAuthentication();

        WebClient.RequestHeadersSpec<?> partial = webClient.get()
                .uri(schemaUrl);
        partial = WebUtils.addBasicAuthSecurity(partial, confluentBrokerAuthentication.getSchemaRegistry());
        // DENNIS TO-DO
        //partial = WebUtils.
        String subjectRaw = partial
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return objectMapper.readValue(subjectRaw, new TypeReference<>() {
        });
    }

    private ConfluentSubject getLatestSchemaForSubject(String subjectName) {
        ServiceIdentity schemaRegId = ((ConfluentKafkaIdentity) getBrokerIdentity()).getSchemaRegistry();
        ServiceAuthentication schemaRegAuth = ((ConfluentBrokerAuthentication) getBrokerAuthentication()).getSchemaRegistry();
        String schemaUrl = WebUtils.getServiceUrl("/subjects/" + subjectName + "/versions/latest", schemaRegId, schemaRegAuth);
        ConfluentBrokerAuthentication confluentBrokerAuthentication = (ConfluentBrokerAuthentication) getBrokerAuthentication();

        WebClient.RequestHeadersSpec<?> partial = webClient
                .get()
                .uri(schemaUrl);
        partial = WebUtils.addBasicAuthSecurity(partial, confluentBrokerAuthentication.getSchemaRegistry());
        return partial
                .retrieve()
                .bodyToMono(ConfluentSubject.class)
                .block();
    }

    /*** Event Normalizer ***/
//    private void normalizeEventAndStoreInEventMap(SubjectNameStrategy subjectNameStrategy, String subject, String topic,
//                                                  String schema, String version, int schemaRegId) {
    private void normalizeEventAndStoreInEventMap(SubjectNameStrategy subjectNameStrategy, String subject, String topic,
                                                  ConfluentSubject confluentSubject) {
        KafkaNormalizedEventImpl normalizedEvent;
        String version = Integer.toString(confluentSubject.getVersion());
        String schemaId = Integer.toString(confluentSubject.getId());

        // uniqueId is used when there are multiple events on the same topic
        String uniqueEventName = topic;

        switch (subjectNameStrategy) {
            case TOPIC:
                break;
            case TOPIC_RECORD:
            case RECORD:
                uniqueEventName = subject;
                break;
        }

        if (topicToNormalizedEventMap.containsKey(uniqueEventName)) {
            normalizedEvent = (KafkaNormalizedEventImpl) topicToNormalizedEventMap.get(uniqueEventName);
        } else {
            normalizedEvent = KafkaNormalizedEventImpl.builder()
                    .normalizedMessage(KafkaNormalizedMessageImpl.builder()
                            .build())
                    .channelName(topic)
                    .channelType("topic")
                    .fromSchemaRegistry(true)
                    .build();
            topicToNormalizedEventMap.put(uniqueEventName, normalizedEvent);
        }

        switch (subjectNameStrategy) {
            // For the topic name strategy put both the key and value in the same
            // normalized event.
            case TOPIC:
                if (subject.endsWith(TOPIC_SUBJECT_VALUE_SUFFIX)) {
                    normalizedEvent.setSchemaId(schemaId);
                    normalizedEvent.setSchema(confluentSubject.getSchema());
                    normalizedEvent.setSchemaType(SchemaType.AVRO);
                    normalizedEvent.setVersion(version);
                    normalizedEvent.setSubjectName(confluentSubject.getSubject());
                } else if (subject.endsWith(TOPIC_SUBJECT_KEY_SUFFIX)) {
                    normalizedEvent.setKeySchema(confluentSubject.getSchema());
                    normalizedEvent.setKeySchemaType(SchemaType.AVRO);
                    normalizedEvent.setKeySchemaVersion(version);
                    normalizedEvent.setKeySchemaId(schemaId);
                    normalizedEvent.setKeySubjectName(confluentSubject.getSubject());
                } else {
                    log.warn("Unexpected subject using TOPIC strategy {}", subject);
                }
                break;
            case TOPIC_RECORD:
            case RECORD:
                // For now assume its the value
                normalizedEvent.setSchemaId(schemaId);
                normalizedEvent.setSchema(confluentSubject.getSchema());
                normalizedEvent.setSchemaType(SchemaType.AVRO);
                normalizedEvent.setVersion(version);
                normalizedEvent.setSubjectName(confluentSubject.getSubject());
                break;
            default:
                log.warn("Unknown subject strategy");
        }
    }

    private String getTopicFromSubject(String subject) {
        int lastDashIndex = subject.lastIndexOf('-');
        return subject.substring(0, lastDashIndex);
    }

    @Override
    public PluginConfigData getPluginConfiguration() {
        return BasicPluginConfigData.builder()
                .pluginName(getPluginName())
                .brokerCapabilities(getBrokerCapabilities())
                .inputSchema(new HashMap<>())
                .brokerAuthenticationClass(ConfluentBrokerAuthentication.class)
                .brokerIdentityClass(ConfluentKafkaIdentity.class)
                .brokerType("CONFLUENT")
                .pluginClass(getClass())
                .build();
    }

    @Override
    protected BrokerCapabilities getBrokerCapabilities() {
        BrokerCapabilities brokerCapabilities = new BrokerCapabilities();
        brokerCapabilities.setCapabilities(Collections.singleton(BrokerCapabilities.Capability.SCHEMA_REGISTRY));
        return brokerCapabilities;
    }

    @Override
    public List<NormalizedEvent> getNormalizedEvents() {
        return topicToNormalizedEventMap.values().stream().collect(Collectors.toUnmodifiableList());
    }

}
