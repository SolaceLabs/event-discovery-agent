package com.event.discovery.agent.integrationTests.offline.kafkaCommon.confluent;

import com.event.discovery.agent.apps.model.EventDiscoveryOperationDiscoveryResultBO;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaAuthentication;
import com.event.discovery.agent.kafkaConfluent.model.ConfluentBrokerAuthentication;
import com.event.discovery.agent.kafkaConfluent.model.ConfluentKafkaIdentity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.event.discovery.agent.rest.model.v1.DiscoveryOperationRequestDTO;
import com.event.discovery.agent.rest.model.v1.DiscoveryOperationResponseDTO;
import com.event.discovery.agent.types.event.common.Channel;
import com.event.discovery.agent.types.event.common.ChannelToSchemaRelationship;
import com.event.discovery.agent.types.event.common.Client;
import com.event.discovery.agent.types.event.common.ClientToChannelRelationship;
import com.event.discovery.agent.types.event.common.ClientToSubscriptionRelationship;
import com.event.discovery.agent.types.event.common.DiscoveryData;
import com.event.discovery.agent.types.event.common.Schema;
import com.event.discovery.agent.types.event.common.Subscription;
import com.event.discovery.agent.framework.model.CommonModelConstants;
import com.event.discovery.agent.framework.model.DiscoveryOperation;
import com.event.discovery.agent.framework.model.JobStatus;
import com.event.discovery.agent.framework.model.ServiceAuthentication;
import com.event.discovery.agent.framework.model.ServiceIdentity;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConfluentEventDiscoveryTestsIT extends ConfluentCommonTestsIT {
    @Test
    public void testConfluentSuccessPath() {
        EventDiscoveryOperationDiscoveryResultBO result = runOperationAndGetResult();
        kafkaResultValidation.validateConnector(result);

        kafkaResultValidation.validateTopic(result, 1);
        validateCommonResponseAttributes(result);
        validateSchemaRegistryAuthentication(result);

        DiscoveryData eventData = (DiscoveryData) result.getEvents();

        assertEquals("0.0.2", eventData.getVersion());

        // Topic
        Channel topic = eventData.getChannels().get(0);
        assertEquals("ATopic", topic.getName());
        assertNotNull(topic.getId());
        assertEquals(CommonModelConstants.Channel.TOPIC,
                getAttributeFromMap(topic.getAdditionalAttributes(), CommonModelConstants.Channel.TYPE));

        // Subscriptions (these will be duplicated from topics, if they are in a relationship)
        assertEquals(1, eventData.getSubscriptions().size());
        Subscription subscription = eventData.getSubscriptions().get(0);
        assertEquals(topic.getName(), subscription.getMatchCriteria());
        assertNotNull(subscription.getId());

        // Connectors
        List<Client> connectorsList = getConnectors(eventData);
        assertEquals(2, connectorsList.size());
        Map<String, Client> connectorsMap = connectorsList.stream()
                .collect(Collectors.toMap(Client::getName, Function.identity()));

        Client connectorProducer = connectorsMap.get("connector1");
        assertEquals("connector1", connectorProducer.getName());
        assertEquals("VeryBestConnector",
                getAttributeFromMap(connectorProducer.getAdditionalAttributes(),
                        CommonModelConstants.Client.CONNECTOR_CLASS_KEY));
        assertEquals("1", getAttributeFromMap(connectorProducer.getAdditionalAttributes(),
                CommonModelConstants.Client.MAX_THREADS));
        assertEquals(CommonModelConstants.Client.TYPE_PRODUCER, connectorProducer.getType());
        assertNotNull(connectorProducer.getId());
        assertEquals(CommonModelConstants.Client.CLIENT_TYPE_CONNECTOR,
                getAttributeFromMap(connectorProducer.getAdditionalAttributes(),
                        CommonModelConstants.Client.CLIENT_TYPE_KEY));

        // Consumer Group
        List<Client> consumerGroups = getConsumerGroups(eventData);
        assertEquals(2, consumerGroups.size());
        Client consumerGroup = consumerGroups.stream()
                .filter(cg -> "AConsumerGroup".equals(cg.getName()))
                .findFirst()
                .get();
        assertEquals("AConsumerGroup", consumerGroup.getName());
        assertEquals(CommonModelConstants.Client.TYPE_CONSUMER, consumerGroup.getType());
        assertEquals(false, getAttributeFromMap(consumerGroup.getAdditionalAttributes(),
                CommonModelConstants.Client.SIMPLE_CONSUMER));
        assertEquals(CommonModelConstants.Client.CLIENT_TYPE_APPLICATION, getAttributeFromMap(consumerGroup.getAdditionalAttributes(),
                CommonModelConstants.Client.CLIENT_TYPE_KEY));
        assertNotNull(consumerGroup.getId());

        // Schemas
        List<Schema> schemaList = eventData.getSchemas();
        assertEquals(1, schemaList.size());
        Schema schema = schemaList.get(0);
        assertEquals("AVRO", schema.getSchemaType());
        assertTrue(schema.getContent().startsWith("{"));
        assertEquals(Boolean.FALSE, schema.getPrimitive());
        assertNotNull(schema.getId());

        assertEquals("0",
                getAttributeFromMap(schema.getAdditionalAttributes(), CommonModelConstants.Schema.SCHEMA_REG_ID));
        assertEquals("1",
                getAttributeFromMap(schema.getAdditionalAttributes(), CommonModelConstants.Schema.SUBJECT_VERSION));
        assertEquals("JSON",
                getAttributeFromMap(schema.getAdditionalAttributes(), CommonModelConstants.Schema.CONTENT_TYPE));


        // TopicToSchema
        List<ChannelToSchemaRelationship> channelToSchemaRelationships = eventData.getChannelToSchemaRelationships();
        assertEquals(1, channelToSchemaRelationships.size());
        assertTrue(hasChannelToSchemaRelationship(channelToSchemaRelationships, topic, schema));

        // Client to subscriptions
        List<ClientToSubscriptionRelationship> clientToSubscriptionRelationships = eventData.getClientToSubscriptionRelationships();
        assertEquals(1, clientToSubscriptionRelationships.size());

        // Consumer Groups
        assertTrue(hasClientToSubscriptionRelationship(clientToSubscriptionRelationships, consumerGroup, subscription));

        // Connectors - only producer
        List<ClientToChannelRelationship> clientToChannelRelationships = eventData.getClientToChannelRelationships();
        assertEquals(1, clientToChannelRelationships.size());
        assertTrue(hasClientToChannelRelationship(clientToChannelRelationships, connectorProducer, topic));

        try {
            System.out.println(new ObjectMapper().writeValueAsString(result));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private Schema findSchemaBySchemaRegId(final List<Schema> schemas, final String regId) {
        return schemas.stream()
                .filter(schema -> regId.equals(
                        getAttributeFromMap(schema.getAdditionalAttributes(),
                                CommonModelConstants.Schema.SCHEMA_REG_ID)))
                .findFirst().get();
    }

    private void validateSchemaRegistryAuthentication(EventDiscoveryOperationDiscoveryResultBO result) {

        Map<String, Object> schemaRegAuth = (Map<String, Object>)
                ((Map<String, Object>) result.getPluginInputs().get("brokerAuthentication")).get("schemaRegistry");
        assertEquals("trustypassword", schemaRegAuth.get("trustStorePassword"));
        assertEquals("there", schemaRegAuth.get("trustStoreLocation"));
        assertEquals("mypass", schemaRegAuth.get("keyStorePassword"));
        assertEquals("here", schemaRegAuth.get("keyStoreLocation"));
        assertEquals("ImSoBasic", schemaRegAuth.get("basicAuthPassword"));
        assertEquals("BasicUser", schemaRegAuth.get("basicAuthUsername"));
    }

    protected void validateCommonResponseAttributes(EventDiscoveryOperationDiscoveryResultBO result) {
        assertTrue(result.getDiscoveryStartTime() > 0);
        assertTrue(result.getDiscoveryEndTime() > 0);
        assertEquals("confluentEventDiscovery", result.getName());
        assertEquals("0.0.2", result.getDiscoverySchemaVersion());
        assertEquals("confluent", result.getPluginType());
        assertEquals("kafka", result.getBrokerType());
        assertEquals(3, result.getPluginInputs().size());
        assertEquals(0, result.getWarnings().size());

    }

    @Test
    public void testTwoEventsSameSchema() {
        setupMocksForTwoEventsSingleSchema();

        EventDiscoveryOperationDiscoveryResultBO result = runOperationAndGetResult();
        kafkaResultValidation.validateConnector(result);
        kafkaResultValidation.validateTopic(result, 2, "BTopic");

        DiscoveryData eventData = (DiscoveryData) result.getEvents();

        // Only expect 1 schema since 2 topics  use the same schema
        List<Schema> schemaList = eventData.getSchemas();
        assertEquals(1, schemaList.size());

        // We expect 2 topic to schema associations
        List<ChannelToSchemaRelationship> channelToSchemaRelationships = eventData.getChannelToSchemaRelationships();
        assertEquals(2, channelToSchemaRelationships.size());
        assertTrue(hasChannelToSchemaRelationship(
                channelToSchemaRelationships, eventData.getChannels().get(0), schemaList.get(0)));
        assertTrue(hasChannelToSchemaRelationship(
                channelToSchemaRelationships, eventData.getChannels().get(1), schemaList.get(0)));
    }

    @Test
    public void testPrimitiveKeyAndValuesFromSchemaReg() {
        setupMocksForPrimitiveKeyAndValueFromRegistry();

        EventDiscoveryOperationDiscoveryResultBO result = runOperationAndGetResult();
        kafkaResultValidation.validateConnector(result);
        kafkaResultValidation.validateTopic(result, 2, "BTopic");

        DiscoveryData eventData = (DiscoveryData) result.getEvents();

        // Only expect 2 schemas
        List<Schema> schemaList = eventData.getSchemas();
        assertEquals(2, schemaList.size());
        Schema valueSchema = findSchemaBySchemaRegId(schemaList, "1");
        assertEquals("\"string\"", valueSchema.getContent());
        assertEquals("AVRO", valueSchema.getSchemaType());
        assertEquals("TEXT",
                getAttributeFromMap(valueSchema.getAdditionalAttributes(), CommonModelConstants.Schema.CONTENT_TYPE));

        Schema keySchema = findSchemaBySchemaRegId(schemaList, "2");
        assertEquals("\"float\"", keySchema.getContent());
        assertEquals("AVRO", keySchema.getSchemaType());
        assertEquals("TEXT",
                getAttributeFromMap(keySchema.getAdditionalAttributes(), CommonModelConstants.Schema.CONTENT_TYPE));

        List<ChannelToSchemaRelationship> channelToSchemaRelationships = eventData.getChannelToSchemaRelationships();
        assertEquals(1, channelToSchemaRelationships.size());
        assertTrue(hasChannelToSchemaRelationship(channelToSchemaRelationships,
                eventData.getChannels().get(0), valueSchema, keySchema));
    }

    private EventDiscoveryOperationDiscoveryResultBO runOperationAndGetResult() {
        DiscoveryOperationResponseDTO response = createEventListenerOperation(
                createDefaultConfluentEventDiscoveryRequest());
        assertNotNull(response.getJobId());

        ConfluentBrokerAuthentication confluentAuth = (ConfluentBrokerAuthentication) response.getBrokerAuthentication();
        if (confluentAuth.getSchemaRegistry() != null) {
            ServiceAuthentication schemaRegAuth = confluentAuth.getSchemaRegistry();
            assertEquals("**************", schemaRegAuth.getTrustStorePassword());
            assertEquals("**************", schemaRegAuth.getKeyStorePassword());
            assertEquals("**************", schemaRegAuth.getBasicAuthPassword());
            assertEquals("there", schemaRegAuth.getTrustStoreLocation());
            assertEquals("here", schemaRegAuth.getKeyStoreLocation());
            assertEquals("BasicUser", schemaRegAuth.getBasicAuthUsername());
        }
        waitUntilJobStatus(JobStatus.COMPLETED, response.getJobId());

        String asyncApiResult = getDiscoveryOperationAsyncAPIResult(response.getJobId());

        // No real async api validation for now since this area of the code is immature and
        // is expected to iterate pretty quickly
        assertNotNull(asyncApiResult);

        return getResultInInternalModel();
    }

    @Test
    public void testPrimitiveKeyAndNoValueFromSchemaReg() {
        setupMocksForPrimitiveKeyAndNoValueFromRegistry();

        EventDiscoveryOperationDiscoveryResultBO result = runOperationAndGetResult();
        kafkaResultValidation.validateConnector(result);

        kafkaResultValidation.validateTopic(result, 2, "BTopic");

        DiscoveryData eventData = (DiscoveryData) result.getEvents();

        // Only expect 0 schemas
        List<Schema> schemaList = eventData.getSchemas();
        assertEquals(0, schemaList.size());

        // We expect 0 topic to schema associations
        List<ChannelToSchemaRelationship> channelToSchemaRelationships = eventData.getChannelToSchemaRelationships();
        assertEquals(0, channelToSchemaRelationships.size());
    }

    @Test
    public void testKeyAndValueComplexSchema() {
        setupMocksForKeyAndValueComplexSchema();

        EventDiscoveryOperationDiscoveryResultBO result = runOperationAndGetResult();
        kafkaResultValidation.validateConnector(result);

        kafkaResultValidation.validateTopic(result, 2, "BTopic");

        DiscoveryData eventData = (DiscoveryData) result.getEvents();

        try {
            System.out.println(objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        // Only expect 2 schemas
        List<Schema> schemaList = eventData.getSchemas();
        assertEquals(2, schemaList.size());
        Schema valueSchema = findSchemaBySchemaRegId(schemaList, "0");
        Schema keySchema = findSchemaBySchemaRegId(schemaList, "1");

        List<ChannelToSchemaRelationship> channelToSchemaRelationships = eventData.getChannelToSchemaRelationships();
        assertEquals(1, channelToSchemaRelationships.size());
        assertTrue(hasChannelToSchemaRelationship(channelToSchemaRelationships,
                eventData.getChannels().get(0), valueSchema, keySchema));
    }

    @Test
    public void testPrimitiveKeyAndValueComplexSchema() {
        setupMocksForPrimitiveKeyAndComplexValueSchema();

        EventDiscoveryOperationDiscoveryResultBO result = runOperationAndGetResult();
        kafkaResultValidation.validateConnector(result);

        kafkaResultValidation.validateTopic(result, 2, "BTopic");

        DiscoveryData eventData = (DiscoveryData) result.getEvents();

        // Only expect 2 schemas
        List<Schema> schemaList = eventData.getSchemas();
        assertEquals(2, schemaList.size());
        Schema valueSchema = findSchemaBySchemaRegId(schemaList, "0");
        Schema keySchema = findSchemaBySchemaRegId(schemaList, "6");

        // We expect 1 topic to schema associations
        List<ChannelToSchemaRelationship> channelToSchemaRelationships = eventData.getChannelToSchemaRelationships();
        assertEquals(1, channelToSchemaRelationships.size());
        assertTrue(hasChannelToSchemaRelationship(channelToSchemaRelationships,
                eventData.getChannels().get(0), valueSchema, keySchema));
    }

    @Test
    public void testTwoEventsSameTopic() {
        setupMocksForTwoEventsSingleTopic();

        EventDiscoveryOperationDiscoveryResultBO result = runOperationAndGetResult();
        kafkaResultValidation.validateConnector(result);
        kafkaResultValidation.validateTopic(result, 2, "BTopic");

        DiscoveryData eventData = (DiscoveryData) result.getEvents();

        Channel topic = eventData.getChannels().stream()
                .filter(channel -> channel.getName().equals("BTopic"))
                .findFirst().get();

        // Expect 2 schemas
        List<Schema> schemaList = eventData.getSchemas();
        assertEquals(2, schemaList.size());
        Schema valueSchema1 = findSchemaBySchemaRegId(schemaList, "0");
        Schema valueSchema2 = findSchemaBySchemaRegId(schemaList, "1");

        // We expect 2 topic to schema associations, both with the same topic
        List<ChannelToSchemaRelationship> channelToSchemaRelationships = eventData.getChannelToSchemaRelationships();
        assertEquals(2, channelToSchemaRelationships.size());
        assertTrue(hasChannelToSchemaRelationship(channelToSchemaRelationships, topic, valueSchema1));
        assertTrue(hasChannelToSchemaRelationship(channelToSchemaRelationships, topic, valueSchema2));
    }

    @Test
    public void testRecordSubjectStrategyWithComplexSchema() {
        setupMocksForRecordSubjectStrategyWithComplexSchema();

        EventDiscoveryOperationDiscoveryResultBO result = runOperationAndGetResult();
        kafkaResultValidation.validateConnector(result);

        kafkaResultValidation.validateTopic(result, 2, "BTopic");

        DiscoveryData eventData = (DiscoveryData) result.getEvents();

        // Expect 1 schemas
        List<Schema> schemaList = eventData.getSchemas();
        assertEquals(1, schemaList.size());
        Schema schema = schemaList.get(0);
        assertEquals("0", getAttributeFromMap(schema.getAdditionalAttributes(), CommonModelConstants.Schema.SCHEMA_REG_ID));

        // We expect 0 topic to schema associations
        List<ChannelToSchemaRelationship> channelToSchemaRelationships = eventData.getChannelToSchemaRelationships();
        assertEquals(0, channelToSchemaRelationships.size());
    }

    @Test
    public void testUnknownTopicToConnectorAssociation() {
        setupMocksForUnknownTopicToConnector();

        EventDiscoveryOperationDiscoveryResultBO result = runOperationAndGetResult();
        kafkaResultValidation.validateConnector(result);
        kafkaResultValidation.validateTopic(result, 1, "ATopic");

        DiscoveryData eventData = (DiscoveryData) result.getEvents();

        List<ClientToChannelRelationship> clientToChannelRelationships =
                eventData.getClientToChannelRelationships();

        assertEquals(0, clientToChannelRelationships.size());
    }

    @Test
    public void testUnknownTopicForConsumerGroupAssociation() throws ExecutionException, InterruptedException {
        setupMocksForUnknownTopicToConsumerGroup();

        EventDiscoveryOperationDiscoveryResultBO result = runOperationAndGetResult();
        kafkaResultValidation.validateConnector(result);
        kafkaResultValidation.validateTopic(result, 1, "ATopic");

        DiscoveryData eventData = (DiscoveryData) result.getEvents();

        List<ClientToSubscriptionRelationship> clientToSubscriptionRelationships =
                eventData.getClientToSubscriptionRelationships();

        assertEquals(0, clientToSubscriptionRelationships.size());
    }

    protected DiscoveryOperationRequestDTO createDefaultConfluentEventDiscoveryRequest() {

        return createConfluentEventDiscoveryRequest(3);
    }

    protected DiscoveryOperationRequestDTO createConfluentEventDiscoveryRequest(int duration) {

        DiscoveryOperationRequestDTO request = new DiscoveryOperationRequestDTO();

        ConfluentKafkaIdentity identity = ConfluentKafkaIdentity.builder()
                .host("someHost")
                .brokerType("CONFLUENT")
                .build();

        identity.setSchemaRegistry(new ServiceIdentity());
        identity.getSchemaRegistry().setHostname("schemaHost");
        identity.getSchemaRegistry().setPort(9093);

        identity.setConnector(new ServiceIdentity());
        identity.getConnector().setHostname("connectors");
        identity.getConnector().setPort(9095);

        ServiceAuthentication schemaRegAuth = new ServiceAuthentication();
        schemaRegAuth.setBasicAuthPassword("ImSoBasic");
        schemaRegAuth.setBasicAuthUsername("BasicUser");
        schemaRegAuth.setKeyStoreLocation("here");
        schemaRegAuth.setKeyStorePassword("mypass");
        schemaRegAuth.setTrustStoreLocation("there");
        schemaRegAuth.setTrustStorePassword("trustypassword");

        ConfluentBrokerAuthentication auth = ConfluentBrokerAuthentication.builder()
                .brokerType("CONFLUENT")
                .authenticationType(ApacheKafkaAuthentication.KafkaAuthentiationType.NOAUTH)
                .transportType(ApacheKafkaAuthentication.KafkaTransportType.PLAINTEXT)
                .schemaRegistry(schemaRegAuth)
                .build();

        DiscoveryOperation discoveryOperation = DiscoveryOperation.noArgsBuilder()
                .messageQueueLength(100000)
                .operationType("eventDiscovery")
                //.subscriptionSet(new HashSet<>(Arrays.asList("aTopicName")))
                .subscriptionSet(Collections.EMPTY_SET)
                .durationInSecs(duration)
                .name("confluentEventDiscovery")
                .build();
        request.setBrokerIdentity(identity);
        request.setBrokerAuthentication(auth);
        request.setDiscoveryOperation(discoveryOperation);
        return request;
    }
}