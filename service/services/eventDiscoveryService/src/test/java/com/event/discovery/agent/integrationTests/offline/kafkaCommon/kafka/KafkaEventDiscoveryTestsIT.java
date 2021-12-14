package com.event.discovery.agent.integrationTests.offline.kafkaCommon.kafka;

import com.event.discovery.agent.apps.model.EventDiscoveryOperationDiscoveryResultBO;
import com.event.discovery.agent.integrationTests.offline.kafkaCommon.KafkaCommonTestsIT;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaAuthentication;
import com.jayway.jsonpath.JsonPath;
import com.event.discovery.agent.rest.model.JobDTO;
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
import com.event.discovery.agent.framework.model.JobStatus;
import com.event.discovery.agent.framework.model.ServiceAuthentication;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.errors.ClusterAuthorizationException;
import org.apache.kafka.common.errors.SecurityDisabledException;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static junit.framework.TestCase.fail;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class KafkaEventDiscoveryTestsIT extends KafkaCommonTestsIT {

    @Test
    public void testBasicKafkaSuccessPath() {
        DiscoveryOperationResponseDTO response = createEventListenerOperation(
                kafkaConfigRequests.createDefaultKafkaEventDiscoveryRequest());

        EventDiscoveryOperationDiscoveryResultBO result = validateResponse(response);
        validateAdminClientProperties(new HashMap<>());

        // Verify only 1 consumergroup to topic association
        DiscoveryData eventData = (DiscoveryData) result.getEvents();
        Channel topic = eventData.getChannels().stream()
                .filter(channel -> channel.getName().equals("ATopic"))
                .findFirst().get();

        Subscription subscriptionAConsumerGroup = eventData.getSubscriptions().stream()
                .filter(sub -> sub.getMatchCriteria().equals("ATopic"))
                .findFirst().get();

        List<Client> consumerGroups = getConsumerGroups(eventData);

        List<ClientToSubscriptionRelationship> consumerGroupToSubscriptionRelationships =
                eventData.getClientToSubscriptionRelationships();

        assertEquals(1, consumerGroupToSubscriptionRelationships.size());
        Client consumerGroup = consumerGroups.stream().filter(cg -> cg.getName().equals("AConsumerGroup"))
                .findFirst().get();

        assertTrue(hasClientToSubscriptionRelationship(consumerGroupToSubscriptionRelationships, consumerGroup, subscriptionAConsumerGroup));

        // Validate connectors
        List<Client> connectors = getConnectors(eventData);

        assertEquals(2, connectors.size());
        Client connector1 = connectors.stream()
                .filter(conn -> conn.getName().equals("connector1"))
                .findFirst().get();
        Client connector2 = connectors.stream()
                .filter(conn -> conn.getName().equals("connector2"))
                .findFirst().get();

        assertEquals("VeryBestConnector",
                getAttributeFromMap(connector1.getAdditionalAttributes(), CommonModelConstants.Client.CONNECTOR_CLASS_KEY));
        assertEquals(CommonModelConstants.Client.TYPE_PRODUCER, connector1.getType());
        assertEquals(CommonModelConstants.Client.TYPE_CONSUMER, connector2.getType());

        // Validate connector to topic association
        List<ClientToChannelRelationship> producerToChannelRelationships =
                eventData.getClientToChannelRelationships();

        assertEquals(1, producerToChannelRelationships.size());
        assertTrue(hasClientToChannelRelationship(producerToChannelRelationships, connector1, topic));
    }

    @Test
    public void testKafkaInactiveConsumer() throws ExecutionException, InterruptedException {
        kafkaAdminClientMocks.setupInactiveConsumer();
        DiscoveryOperationResponseDTO response = createEventListenerOperation(
                kafkaConfigRequests.createDefaultKafkaEventDiscoveryRequest());

        EventDiscoveryOperationDiscoveryResultBO result = validateResponse(response);

        validateAdminClientProperties(new HashMap<>());

        // We expect 2 consumergroup to topic associations
        DiscoveryData eventData = (DiscoveryData) result.getEvents();
        Subscription subscriptionA = eventData.getSubscriptions().stream()
                .filter(sub -> sub.getMatchCriteria().equals("ATopic"))
                .findFirst().get();
        Subscription subscriptionB = eventData.getSubscriptions().stream()
                .filter(sub -> sub.getMatchCriteria().equals("BTopic"))
                .findFirst().get();

        List<Client> consumerGroups = getConsumerGroups(eventData);
        Set<String> consumerGroupIds = consumerGroups.stream()
                .map(Client::getId)
                .collect(Collectors.toSet());
        Client consumerGroupA = consumerGroups.stream()
                .filter(cg -> cg.getName().equals("AConsumerGroup"))
                .findFirst().get();
        Client consumerGroupB = consumerGroups.stream()
                .filter(cg -> cg.getName().equals("BConsumerGroup"))
                .findFirst().get();

        List<ClientToSubscriptionRelationship> consumerGroupToSubscriptions =
                eventData.getClientToSubscriptionRelationships().stream()
                        .filter(rel -> consumerGroupIds.contains(rel.getClientId()))
                        .collect(Collectors.toList());
        assertEquals(2, consumerGroupToSubscriptions.size());
        assertTrue(hasClientToSubscriptionRelationship(consumerGroupToSubscriptions, consumerGroupA, subscriptionA));
        assertTrue(hasClientToSubscriptionRelationship(consumerGroupToSubscriptions, consumerGroupB, subscriptionB));
    }

    @Test
    public void testExceptionForKafkaInactiveConsumer() throws ExecutionException, InterruptedException {
        kafkaAdminClientMocks.setupExceptionForInactiveConsumer();
        DiscoveryOperationResponseDTO response = createEventListenerOperation(
                kafkaConfigRequests.createDefaultKafkaEventDiscoveryRequest());

        EventDiscoveryOperationDiscoveryResultBO result = validateResponse(response);

        validateAdminClientProperties(new HashMap<>());

        // We expect 2 consumer group to topic associations
        DiscoveryData eventData = (DiscoveryData) result.getEvents();
        List<Client> consumerGroups = getConsumerGroups(eventData);
        Set<String> consumerGroupIds = consumerGroups.stream()
                .map(Client::getId)
                .collect(Collectors.toSet());

        List<ClientToSubscriptionRelationship> consumerGroupToSubscriptions =
                eventData.getClientToSubscriptionRelationships().stream()
                        .filter(rel -> consumerGroupIds.contains(rel.getClientId()))
                        .collect(Collectors.toList());
        assertEquals(1, consumerGroupToSubscriptions.size());

        Client consumerGroupA = consumerGroups.stream()
                .filter(cg -> cg.getName().equals("AConsumerGroup"))
                .findFirst().get();
        Subscription subscriptionA = eventData.getSubscriptions().stream()
                .filter(sub -> sub.getMatchCriteria().equals("ATopic"))
                .findFirst().get();

        assertTrue(hasClientToSubscriptionRelationship(consumerGroupToSubscriptions, consumerGroupA, subscriptionA));
    }


    private void validateAdminClientProperties(Map<String, Object> additionalProperties) {
        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("request.timeout.ms", "30000");
        expectedMap.put("bootstrap.servers", "someHost");
        expectedMap.put("client.id", "solace_admin_client");
        expectedMap.putAll(additionalProperties);

        // Make sure all of the expected properties are in the client properties
        for (Map.Entry<String, Object> expectedEntry : expectedMap.entrySet()) {
            Object actualValue = kafkaAdminClientMocks.getAdminClientProperties().get(expectedEntry.getKey());
            assertEquals("Missing client property or incorrect value for property " + expectedEntry.getKey(),
                    expectedEntry.getValue(), actualValue);
        }

        // Make sure all the client properties are in the expected properties
        for (Map.Entry<Object, Object> actualEntry : kafkaAdminClientMocks.getAdminClientProperties().entrySet()) {
            Object expectedValue = expectedMap.get(actualEntry.getKey());
            assertEquals("Extraneous client property or incorrect value for property "
                    + actualEntry.getKey(), expectedValue, actualEntry.getValue());
        }
    }

    @Test
    public void testBasicKafkaSuccessPathSASLPlainSSL() {
        final String obfuscatedPassword = "**************";

        DiscoveryOperationResponseDTO response = createEventListenerOperation(
                kafkaConfigRequests.createKafkaSASLPlainSSLEventDiscoveryRequest(3));

        EventDiscoveryOperationDiscoveryResultBO result = validateResponse(response);

        // Verify obfuscation on response
        ApacheKafkaAuthentication auth = (ApacheKafkaAuthentication) response.getBrokerAuthentication();
        assertEquals("admin123", auth.getAdminUsername());
        assertEquals(obfuscatedPassword, auth.getAdminPassword());
        assertEquals("consumer123", auth.getConsumerUsername());
        assertEquals(obfuscatedPassword, auth.getConsumerPassword());
        assertEquals(obfuscatedPassword, auth.getTrustStorePassword());
        assertEquals(obfuscatedPassword, auth.getKeyStorePassword());
        assertEquals(obfuscatedPassword, auth.getKeyPassword());

        ServiceAuthentication connectorAuth = auth.getConnector();
        assertEquals(obfuscatedPassword, connectorAuth.getTrustStorePassword());
        assertEquals("there", connectorAuth.getTrustStoreLocation());
        assertEquals(obfuscatedPassword, connectorAuth.getKeyStorePassword());
        assertEquals("here", connectorAuth.getKeyStoreLocation());
        assertEquals(obfuscatedPassword, connectorAuth.getBasicAuthPassword());
        assertEquals("BasicUser", connectorAuth.getBasicAuthUsername());

        Map<String, Object> adminClientProps = new HashMap<>();
        adminClientProps.put("sasl.mechanism", "PLAIN");
        adminClientProps.put("ssl.key.password", "rewrwer");
        adminClientProps.put("sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"admin123\" password=\"adminpass\";");
        setSSLExpectedClientProperties(adminClientProps);
        validateAdminClientProperties(adminClientProps);

        Map<String, Object> pluginInputs = (Map<String, Object>) result.getPluginInputs().get("brokerAuthentication");
        assertEquals("admin123", pluginInputs.get("adminUsername"));
        assertEquals("adminpass", pluginInputs.get("adminPassword"));
        assertEquals("consumer123", pluginInputs.get("consumerUsername"));
        assertEquals("consumerpass", pluginInputs.get("consumerPassword"));
        assertEquals("qwerqwer", pluginInputs.get("trustStorePassword"));
        assertEquals("gdgsgsd", pluginInputs.get("keyStorePassword"));
        assertEquals("rewrwer", pluginInputs.get("keyPassword"));

        Map<String, Object> connAuth = (Map<String, Object>) pluginInputs.get("connector");
        assertEquals("trustypassword", connAuth.get("trustStorePassword"));
        assertEquals("there", connAuth.get("trustStoreLocation"));
        assertEquals("mypass", connAuth.get("keyStorePassword"));
        assertEquals("here", connAuth.get("keyStoreLocation"));
        assertEquals("ImSoBasic", connAuth.get("basicAuthPassword"));
        assertEquals("BasicUser", connAuth.get("basicAuthUsername"));
    }

    @Test
    public void testBasicKafkaSuccessPathSASLGSSAPISSL() {
        DiscoveryOperationResponseDTO response = createEventListenerOperation(
                kafkaConfigRequests.createKafkaSASLGSSAPISSLEventDiscoveryRequest(3));

        validateResponse(response);

        Map<String, Object> adminClientProps = new HashMap<>();
        adminClientProps.put("sasl.mechanism", "GSSAPI");
        adminClientProps.put("ssl.key.password", "rewrwer");
        adminClientProps.put("sasl.jaas.config",
                "com.sun.security.auth.module.Krb5LoginModule required\n" +
                        "    serviceName=\"myService1\"\n" +
                        "    useKeyTab=true\n" +
                        "    storeKey=true\n" +
                        "    keyTab=\"/Users/user/confluentDemo/5.5/saslconsumer.keytab\"\n" +
                        "    principal=\"saslconsumer/confluent.mymaas.net@TEST.CONFLUENT.IO\";");
        setSSLExpectedClientProperties(adminClientProps);
        validateAdminClientProperties(adminClientProps);
    }

    @Test
    public void testBasicKafkaSuccessPathSASLSCRAM256SSL() {
        DiscoveryOperationResponseDTO response = createEventListenerOperation(
                kafkaConfigRequests.createKafkaSASLSCRAM256SSLEventDiscoveryRequest(3));

        validateResponse(response);

        Map<String, Object> adminClientProps = new HashMap<>();
        adminClientProps.put("sasl.mechanism", "SCRAM-SHA-256");
        adminClientProps.put("ssl.key.password", "rewrwer");
        adminClientProps.put("sasl.jaas.config",
                "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"kafkaclient2\" password=\"password1234\";");
        setSSLExpectedClientProperties(adminClientProps);
        validateAdminClientProperties(adminClientProps);
    }

    @Test
    public void testBasicKafkaSuccessPathSASLSCRAM512SSL() {
        DiscoveryOperationResponseDTO response = createEventListenerOperation(
                kafkaConfigRequests.createKafkaSASLSCRAM512SSLEventDiscoveryRequest(3));

        validateResponse(response);

        Map<String, Object> adminClientProps = new HashMap<>();
        adminClientProps.put("sasl.mechanism", "SCRAM-SHA-512");
        adminClientProps.put("ssl.key.password", "rewrwer");
        adminClientProps.put("sasl.jaas.config",
                "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"kafkaclient2\" password=\"password1234\";");
        setSSLExpectedClientProperties(adminClientProps);
        validateAdminClientProperties(adminClientProps);
    }

    @Test
    public void testAdminClientError() {
        AdminClient adminClient = kafkaAdminClientMocks.getAdminClientObjectProvider().getObject(new Properties());

        when(adminClient.listTopics()).thenThrow(new RuntimeException());
        DiscoveryOperationResponseDTO response = createEventListenerOperation(
                kafkaConfigRequests.createDefaultKafkaEventDiscoveryRequest());

        String jobId = response.getJobId();
        final JobDTO status = getDiscoveryOperationStatus(jobId);
        assertEquals(JobStatus.ERROR, status.getStatus());

        getAndThen(APPLICATION_OPERATION_STATUS_URL, jobId)
                .statusCode(HttpStatus.OK.value())
                .body("data.status", CoreMatchers.is("ERROR"));
    }

    @Test
    public void testAclDisabled() throws Exception {
        KafkaFuture<Collection<AclBinding>> future = new KafkaFutureImpl<>() {
            @Override
            public Collection<AclBinding> get() throws InterruptedException, ExecutionException {
                // We will ignore the SecurityDisabledException and keep on going returning no acls
                throw new ExecutionException("doesn't matter", new SecurityDisabledException("also doesn't matter"));
            }
        };

        executeAclTest(future);
    }

    @Test
    public void testAclDAuthorizationError() throws Exception {
        KafkaFuture<Collection<AclBinding>> future = new KafkaFutureImpl<>() {
            @Override
            public Collection<AclBinding> get() throws InterruptedException, ExecutionException {
                // We will ignore the SecurityDisabledException and keep on going returning no acls
                throw new ExecutionException("doesn't matter", new ClusterAuthorizationException("also doesn't matter"));
            }
        };

        executeAclTest(future);
    }

    @Test
    public void testAclError() throws Exception {
        KafkaFuture<Collection<AclBinding>> future = new KafkaFutureImpl<>() {
            @Override
            public Collection<AclBinding> get() throws InterruptedException, ExecutionException {
                throw new ExecutionException(new IllegalStateException());
            }
        };

        kafkaAdminClientMocks.configureDescribeAclResult(future);

        DiscoveryOperationResponseDTO response = createEventListenerOperation(
                kafkaConfigRequests.createDefaultKafkaEventDiscoveryRequest());

        final String jobId = response.getJobId();
        assertNotNull(jobId);

        final JobDTO status = getDiscoveryOperationStatus(jobId);
        assertThat(status.getStatus(), is(JobStatus.ERROR));
    }

    @Test
    public void testAclUnsupportedVersionHandledGracefully() throws Exception {
        KafkaFuture<Collection<AclBinding>> future = new KafkaFutureImpl<>() {
            @Override
            public Collection<AclBinding> get() throws InterruptedException, ExecutionException {
                throw new ExecutionException(new UnsupportedVersionException("The broker does not support DESCRIBE_ACLS"));
            }
        };

        kafkaAdminClientMocks.configureDescribeAclResult(future);

        DiscoveryOperationResponseDTO response = createEventListenerOperation(
                kafkaConfigRequests.createDefaultKafkaEventDiscoveryRequest());

        final String jobId = response.getJobId();
        assertNotNull(jobId);

        final JobDTO status = getDiscoveryOperationStatus(jobId);
        assertThat(status.getStatus(), is(JobStatus.COMPLETED));
    }

    private void setSSLExpectedClientProperties(Map<String, Object> adminClientProps) {
        adminClientProps.put("ssl.truststore.password", "qwerqwer");
        adminClientProps.put("ssl.keystore.location", "/e/f/c/h");
        adminClientProps.put("security.protocol", "SASL_SSL");
        adminClientProps.put("ssl.truststore.location", "a/b/c/d");
        adminClientProps.put("ssl.keystore.password", "gdgsgsd");
    }


    private void executeAclTest(KafkaFuture<Collection<AclBinding>> future) {
        kafkaAdminClientMocks.configureDescribeAclResult(future);

        DiscoveryOperationResponseDTO response = createEventListenerOperation(
                kafkaConfigRequests.createDefaultKafkaEventDiscoveryRequest());

        final String jobId = response.getJobId();
        assertNotNull(jobId);

        waitUntilJobStatus(JobStatus.COMPLETED, jobId);

        String asyncApiResult = getDiscoveryOperationAsyncAPIResult(jobId);

        // No real async api validation for now since this area of the code is immature and
        // is expected to iterate pretty quickly
        assertNotNull(asyncApiResult);

        EventDiscoveryOperationDiscoveryResultBO result = getResultInInternalModel();

        if (discoveryProperties.isIncludeConfiguration()) {
            List<Map<String, Object>> acls = (List<Map<String, Object>>) result.getConfiguration().get("kafka_acl");
            assertThat(acls.size(), is(0));
        }
    }

    @Test
    public void testPollError() {
        when(kafkaConsumer.poll(any(Duration.class))).thenThrow(new IllegalStateException());
        DiscoveryOperationResponseDTO response = createEventListenerOperation(
                kafkaConfigRequests.createDefaultKafkaEventDiscoveryRequest());

        String jobId = response.getJobId();
        final JobDTO status = getDiscoveryOperationStatus(jobId);
        assertEquals(JobStatus.ERROR, status.getStatus());

        getAndThen(APPLICATION_OPERATION_STATUS_URL, jobId)
                .statusCode(HttpStatus.OK.value())
                .body("data.status", CoreMatchers.is("ERROR"));
    }

    private String getSchemaTitle(final Schema schema) {
        return JsonPath.read(schema.getContent(), "$.title");
    }

    private EventDiscoveryOperationDiscoveryResultBO validateResponse(DiscoveryOperationResponseDTO response) {
        final String jobId = response.getJobId();
        assertNotNull(jobId);

        waitUntilJobStatus(JobStatus.COMPLETED, jobId);
        String asyncApiResult = getDiscoveryOperationAsyncAPIResult(jobId);

        // No real async api validation for now since this area of the code is immature and
        // is expected to iterate pretty quickly
        assertNotNull(asyncApiResult);

        EventDiscoveryOperationDiscoveryResultBO result = getResultInInternalModel();

        // Verify the cluster data
        if (discoveryProperties.isIncludeConfiguration()) {
            Map<String, Object> cluster = (Map<String, Object>) result.getConfiguration().get("cluster");
            assertThat(cluster.get("cluster_id"), is("ThisIsMyClusterId"));
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) cluster.get("nodes");
            assertEquals(3, nodes.size());
        }

        DiscoveryData eventData = (DiscoveryData) result.getEvents();

        Map<String, Channel> channelMap = eventData.getChannels().stream()
                .collect(Collectors.toMap(Channel::getId, Function.identity()));

        // Expect 2 schemas
        List<Schema> schemaList = eventData.getSchemas();
        assertEquals(2, schemaList.size());

        Map<String, Schema> schemaTitleMap = new HashMap<>();
        Map<String, Schema> schemaIdMap = new HashMap<>();

        schemaList.forEach(schema -> {
            schemaTitleMap.put(getSchemaTitle(schema), schema);
            schemaIdMap.put(schema.getId(), schema);
        });


        assertThat(schemaTitleMap.keySet(), containsInAnyOrder("ATopic", "BTopic"));

        // We expect 2 topic to schema associations, both with the same topic
        List<ChannelToSchemaRelationship> channelToSchemaRelationships = eventData.getChannelToSchemaRelationships();
        assertEquals(2, channelToSchemaRelationships.size());
        List<String> topics = channelToSchemaRelationships.stream()
                .map(c2s -> channelMap.get(c2s.getChannelId()))
                .map(Channel::getName)
                .collect(Collectors.toList());
        assertThat(topics, containsInAnyOrder("ATopic", "BTopic"));

        assertEquals("{\n  \"definitions\": {},\n  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                        "  \"type\": \"object\",\n  \"title\": \"ATopic\",\n  \"properties\": {\n    \"testattribute\": {\n" +
                        "      \"$id\": \"#/properties/testattribute\",\n      \"title\": \"testattribute schema\",\n" +
                        "      \"type\": \"string\"\n    },\n    \"ted\": {\n      \"$id\": \"#/properties/ted\",\n" +
                        "      \"title\": \"ted schema\",\n      \"type\": \"array\"\n    },\n    \"bill\": {\n" +
                        "      \"$id\": \"#/properties/bill\",\n      \"title\": \"bill schema\",\n      \"type\": \"array\"\n" +
                        "    }\n  }\n}",
                schemaTitleMap.get("ATopic").getContent());

        return result;
    }

    @Test
    public void testKafkaWithStringKeyAndPayloadSuccessPath() {
        // Setup the record with a key and value
        String key = "this key is a string";
        String value = "this value is a string";
        consumeRecords("ATopic", key, value);

        EventDiscoveryOperationDiscoveryResultBO result = launchDiscovery();
        DiscoveryData eventData = (DiscoveryData) result.getEvents();

        Channel topic = eventData.getChannels().stream()
                .filter(channel -> channel.getName().equals("ATopic"))
                .findFirst().get();

        // Expect 2 schemas
        List<Schema> schemaList = eventData.getSchemas();
        assertEquals(2, schemaList.size());

        Map<String, Schema> schemaIdMap = schemaList.stream()
                .collect(Collectors.toMap(Schema::getId, Function.identity()));

        schemaList.forEach(schema -> {
            assertEquals("TEXT", schema.getSchemaType());
            assertEquals("TEXT",
                    getAttributeFromMap(schema.getAdditionalAttributes(), CommonModelConstants.Schema.CONTENT_TYPE));
            assertNotNull(schema.getId());
            assertTrue(schema.getPrimitive());
            assertNull(schema.getContent());
        });

        // We expect 1 topic to schema associations
        List<ChannelToSchemaRelationship> channelToSchemaRelationships = eventData.getChannelToSchemaRelationships();
        assertEquals(1, channelToSchemaRelationships.size());

        ChannelToSchemaRelationship channelToSchemaRelationship = channelToSchemaRelationships.get(0);
        assertNotNull(channelToSchemaRelationship.getChannelId());
        assertEquals(channelToSchemaRelationship.getChannelId(), topic.getId());
        assertNotNull(channelToSchemaRelationship.getSchemaId());
        String keySchemaId = getAttributeFromMap(channelToSchemaRelationship.getAdditionalAttributes(),
                CommonModelConstants.ChannelToSchemaRelationship.KEY_SCHEMA_ID);
        assertNotNull(keySchemaId);

        assertNotEquals(channelToSchemaRelationship.getSchemaId(), keySchemaId);
        assertNotNull(schemaIdMap.get(channelToSchemaRelationship.getSchemaId()));
        assertNotNull(schemaIdMap.get(keySchemaId));
    }

    @Test
    public void testKafkaWithJsonKeyAndPayloadSuccessPath() {
        // Setup the record with a key and a value
        String key = "{\"lat\": \"152\", \"lng\": \"54\", \"name\": \"kanata\"}";
        String value = "{\"driverId\": \"driver1\", \"location\": {\"lat\": \"152\", \"lng\": \"54\", \"name\": \"kanata\"}}";
        String keySchemaContent = "{\n  \"definitions\": {},\n  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n  \"type\": \"object\",\n  " +
                "\"title\": \"ATopic\",\n  \"properties\": {\n    \"lat\": {\n      \"$id\": \"#/properties/lat\",\n      \"title\": \"lat schema\",\n      " +
                "\"type\": \"string\"\n    },\n    \"lng\": {\n      \"$id\": \"#/properties/lng\",\n      \"title\": \"lng schema\",\n      " +
                "\"type\": \"string\"\n    },\n    \"name\": {\n      \"$id\": \"#/properties/name\",\n      \"title\": \"name schema\",\n      " +
                "\"type\": \"string\"\n    }\n  }\n}";
        String valueSchemaContent = "{\n  \"definitions\": {},\n  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n  \"type\": \"object\",\n  " +
                "\"title\": \"ATopic\",\n  \"properties\": {\n    \"driverId\": {\n      \"$id\": \"#/properties/driverId\",\n      " +
                "\"title\": \"driverId schema\",\n      \"type\": \"string\"\n    },\n    \"location\": {\n      \"$id\": \"#/properties/location\",\n      " +
                "\"title\": \"location schema\",\n      \"type\": \"object\",\n      \"properties\": {\n        \"lat\": {\n          " +
                "\"$id\": \"#/properties/lat\",\n          \"title\": \"lat schema\",\n          \"type\": \"string\"\n        },\n        " +
                "\"lng\": {\n          \"$id\": \"#/properties/lng\",\n          \"title\": \"lng schema\",\n          \"type\": \"string\"\n        " +
                "},\n        \"name\": {\n          \"$id\": \"#/properties/name\",\n          \"title\": \"name schema\",\n          " +
                "\"type\": \"string\"\n        }\n      }\n    }\n  }\n}";
        consumeRecords("ATopic", key, value);

        EventDiscoveryOperationDiscoveryResultBO result = launchDiscovery();
        DiscoveryData eventData = (DiscoveryData) result.getEvents();

        // Expect 2 schemas
        List<Schema> schemaList = eventData.getSchemas();
        Schema valueSchema = schemaList.stream()
                .filter(s -> s.getContent().equals(valueSchemaContent))
                .findFirst().get();
        Schema keySchema = schemaList.stream()
                .filter(s -> s.getContent().equals(keySchemaContent))
                .findFirst().get();
        schemaList.forEach(schema -> {
            assertNotNull(schema.getId());
            assertEquals("JSONSCHEMA", schema.getSchemaType());
            assertFalse(schema.getPrimitive());
            assertEquals("JSON",
                    getAttributeFromMap(schema.getAdditionalAttributes(), CommonModelConstants.Schema.CONTENT_TYPE));
        });

        // We expect 1 topic to schema associations
        List<ChannelToSchemaRelationship> channelToSchemaRelationships = eventData.getChannelToSchemaRelationships();
        assertEquals(1, channelToSchemaRelationships.size());

        Channel topic = eventData.getChannels().stream()
                .filter(channel -> channel.getName().equals("ATopic"))
                .findFirst().get();

        assertTrue(hasChannelToSchemaRelationship(channelToSchemaRelationships, topic, valueSchema, keySchema));
    }

    private EventDiscoveryOperationDiscoveryResultBO launchDiscovery() {
        DiscoveryOperationResponseDTO response = createEventListenerOperation(
                kafkaConfigRequests.createDefaultKafkaEventDiscoveryRequest());
        assertNotNull(response.getJobId());

        // Wait for the messages to be processed
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            fail();
        }

        stopDiscovery(response.getJobId());

        getDiscoveryOperationAsyncAPIResult(response.getJobId());
        return getResultInInternalModel();
    }
}
