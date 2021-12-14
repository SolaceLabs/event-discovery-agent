package com.event.discovery.agent.kafkaCommon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.event.discovery.agent.types.config.Cluster;
import com.event.discovery.agent.types.config.KafkaAcl;
import com.event.discovery.agent.types.config.KafkaAclEntry;
import com.event.discovery.agent.types.config.KafkaAclPattern;
import com.event.discovery.agent.types.config.KafkaDiscoveryData;
import com.event.discovery.agent.types.config.Node;
import com.event.discovery.agent.types.config.ResourcePattern;
import com.event.discovery.agent.types.config.Topic;
import com.event.discovery.agent.types.config.TopicConfig;
import com.event.discovery.agent.types.config.TopicConfigValuesInteger;
import com.event.discovery.agent.types.config.TopicConfigValuesNumber;
import com.event.discovery.agent.types.event.common.Channel;
import com.event.discovery.agent.types.event.common.Client;
import com.event.discovery.agent.types.event.common.ClientToChannelRelationship;
import com.event.discovery.agent.types.event.common.ClientToSubscriptionRelationship;
import com.event.discovery.agent.types.event.common.DiscoveryData;
import com.event.discovery.agent.types.event.common.Subscription;
import com.event.discovery.agent.framework.model.BrokerConfigData;
import com.event.discovery.agent.framework.model.DiscoveryOperation;
import com.event.discovery.agent.framework.model.PluginObjectMapper;
import com.event.discovery.agent.framework.utils.IDGenerator;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaAuthentication;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaIdentity;
import com.event.discovery.agent.kafkaCommon.model.KafkaCommonProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeAclsResult;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.ClusterAuthorizationException;
import org.apache.kafka.common.errors.SecurityDisabledException;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Slf4j
public class KafkaCommon {
    private final ObjectMapper objectMapper;
    private final ObjectProvider<AdminClient> adminClientObjectProvider;

    private final KafkaDiscoveryData kafkaConfigDiscoveryData = new KafkaDiscoveryData();
    private final DiscoveryData discoveryData = new DiscoveryData();
    private AdminClient adminClient;
    private ApacheKafkaIdentity brokerIdentity;
    private ApacheKafkaAuthentication brokerAuthentication;
    private DiscoveryOperation discoveryOperation;
    private final KafkaConsumerGroupUtils kafkaConsumerGroupUtils;
    private final KafkaConnectorUtils kafkaConnectorUtils;
    private final IDGenerator idGenerator;
    private final List<String> topics = new ArrayList<>();
    private final Map<String, Channel> channelMap = new HashMap<>();
    private final Map<String, Subscription> channelIdToSubscriptionMap = new HashMap<>();

    public KafkaCommon(
            PluginObjectMapper pluginObjectMapper,
            ObjectProvider<AdminClient> adminClientObjectProvider,
            ObjectProvider<WebClient> webClientObjectProvider, KafkaCommonProperties kafkaCommonProperties,
            IDGenerator idGenerator) {
        objectMapper = pluginObjectMapper.getObjectMapper();
        this.adminClientObjectProvider = adminClientObjectProvider;
        this.idGenerator = idGenerator;
        kafkaConsumerGroupUtils = new KafkaConsumerGroupUtils(this);
        kafkaConnectorUtils = new KafkaConnectorUtils(this, objectMapper, kafkaCommonProperties, webClientObjectProvider);
    }

    public void initialize(ApacheKafkaIdentity brokerIdentity,
                           ApacheKafkaAuthentication brokerAuthentication,
                           DiscoveryOperation discoveryOperation) {
        this.brokerIdentity = brokerIdentity;
        this.brokerAuthentication = brokerAuthentication;
        this.discoveryOperation = discoveryOperation;
    }

    public String getNextGeneratedId() {
        return idGenerator.generateRandomUniqueId("1");
    }

    public void createAdminClient() {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokerIdentity.getHost());
        properties.put(AdminClientConfig.CLIENT_ID_CONFIG, "solace_admin_client");
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "30000");

        setKafkaClientProperties(properties, true);

        adminClient = adminClientObjectProvider.getObject(properties);
    }

    public void setKafkaClientProperties(Properties properties, boolean adminClient) {
        String username;
        String password;

        if (adminClient) {
            username = brokerAuthentication.getAdminUsername();
            password = brokerAuthentication.getAdminPassword();
        } else {
            username = brokerAuthentication.getConsumerUsername();
            password = brokerAuthentication.getConsumerPassword();
        }
        if (brokerAuthentication.getAuthenticationType().equals(ApacheKafkaAuthentication.KafkaAuthentiationType.SASL_PLAIN)) {
            properties.put("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
                    + username + "\" password=\""
                    + password + "\";");
            properties.put("sasl.mechanism", "PLAIN");
            setKafkaSecurityProtocol(properties);
        } else if (brokerAuthentication.getAuthenticationType().equals(ApacheKafkaAuthentication.KafkaAuthentiationType.SASL_SCRAM_256)) {
            properties.put("sasl.jaas.config", "org.apache.kafka.common.security.scram.ScramLoginModule required username=\""
                    + username + "\" password=\""
                    + password + "\";");
            properties.put("sasl.mechanism", "SCRAM-SHA-256");
            setKafkaSecurityProtocol(properties);
        } else if (brokerAuthentication.getAuthenticationType().equals(ApacheKafkaAuthentication.KafkaAuthentiationType.SASL_SCRAM_512)) {
            properties.put("sasl.jaas.config", "org.apache.kafka.common.security.scram.ScramLoginModule required username=\""
                    + username + "\" password=\""
                    + password + "\";");
            properties.put("sasl.mechanism", "SCRAM-SHA-512");
            setKafkaSecurityProtocol(properties);
        } else if (brokerAuthentication.getAuthenticationType().equals(ApacheKafkaAuthentication.KafkaAuthentiationType.SASL_GSSAPI)) {
            System.setProperty("java.security.krb5.conf", brokerAuthentication.getKrb5ConfigurationLocation());
            properties.put("sasl.jaas.config",
                    "com.sun.security.auth.module.Krb5LoginModule required\n" +
                            "    serviceName=\"" + brokerAuthentication.getServiceName() + "\"\n" +
                            "    useKeyTab=true\n" +
                            "    storeKey=true\n" +
                            "    keyTab=\"" + brokerAuthentication.getKeyTabLocation() + "\"\n" +
                            "    principal=\"" + brokerAuthentication.getPrincipal() + "\";");
            properties.put("sasl.mechanism", "GSSAPI");
            setKafkaSecurityProtocol(properties);
        } else if (brokerAuthentication.getAuthenticationType().equals(ApacheKafkaAuthentication.KafkaAuthentiationType.NOAUTH) &&
                brokerAuthentication.getTransportType().equals(ApacheKafkaAuthentication.KafkaTransportType.SSL)) {
            properties.put("security.protocol", "SSL");
        }

        // Add SSL configuration properties
        if (brokerAuthentication.getTransportType().equals(ApacheKafkaAuthentication.KafkaTransportType.SSL)) {
            properties.put("ssl.truststore.location", brokerAuthentication.getTrustStoreLocation());
            if (brokerAuthentication.getTrustStorePassword() != null) {
                properties.put("ssl.truststore.password", brokerAuthentication.getTrustStorePassword());
            }

            if (brokerAuthentication.getKeyStoreLocation() != null) {
                properties.put("ssl.keystore.location", brokerAuthentication.getKeyStoreLocation());
                properties.put("ssl.keystore.password", brokerAuthentication.getKeyStorePassword());
                properties.put("ssl.key.password", brokerAuthentication.getKeyPassword());
            }
        }
    }

    private void setKafkaSecurityProtocol(Properties properties) {
        if (brokerAuthentication.getTransportType().equals(ApacheKafkaAuthentication.KafkaTransportType.PLAINTEXT)) {
            properties.put("security.protocol", "SASL_PLAINTEXT");
        } else if (brokerAuthentication.getTransportType().equals(ApacheKafkaAuthentication.KafkaTransportType.SSL)) {
            properties.put("security.protocol", "SASL_SSL");
        }
    }

    public void cleanup() {
        if (adminClient != null) {
            adminClient.close();
        }
    }

    public BrokerConfigData buildKafkaDiscoveryData() throws ExecutionException, InterruptedException, IOException {
        // Get Cluster Information
        discoveryKafkaCluster();

        // Get Topics
        discoveryKafkaTopics();

        // Get Consumer Groups
        kafkaConsumerGroupUtils.discoveryKafkaConsumerGroups();

        // Get Acls
        discoveryKafkaAcls();

        // Get Connectors
        if (kafkaConnectorUtils.getConnectorClient() != null) {
            kafkaConnectorUtils.discoveryKafkaConnectors();
        }

        // Build the consumer group subscriptions
        kafkaConsumerGroupUtils.getConsumerSubscriptions();

        return BrokerConfigData.builder()
                .configuration(objectMapper.convertValue(kafkaConfigDiscoveryData, new TypeReference<>() {
                }))
                .commonDiscoveryData(discoveryData)
                .build();
    }

    private void discoveryKafkaCluster() throws InterruptedException, ExecutionException {
        DescribeClusterResult cluster = adminClient.describeCluster();
        Cluster clusterModel = new Cluster();
        clusterModel.setClusterId(cluster.clusterId().get());
        Collection<org.apache.kafka.common.Node> nodes = cluster.nodes().get();
        List<Node> clusterNodes = new ArrayList<>();
        for (org.apache.kafka.common.Node node : nodes) {
            Node clusterNode = new Node();
            clusterNode.setId(node.id());
            clusterNode.setHost(node.host());
            clusterNode.setPort(node.port());
            clusterNodes.add(clusterNode);
        }
        clusterModel.setNodes(clusterNodes);
        kafkaConfigDiscoveryData.setCluster(clusterModel);
    }

    private void discoveryKafkaTopics() throws InterruptedException, ExecutionException {
        List<String> topicNameList = adminClient.listTopics().listings().get().stream()
                .map(TopicListing::name)
                .filter(this::topicNameMatchesSubscriptionSet)
                .collect(Collectors.toCollection(ArrayList::new));
        for (String topicName : topicNameList) {
            Topic discoveredTopic = new Topic();
            discoveredTopic.setTopicName(topicName);
            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
            DescribeConfigsResult describeConfigsResult = adminClient.describeConfigs(Collections.singleton(resource));
            Map<ConfigResource, Config> configMap = describeConfigsResult.all().get();
            mapTopicConfigToDiscoveryData(discoveredTopic, configMap.get(resource));
            kafkaConfigDiscoveryData.getTopic().add(discoveredTopic);
            addChannelTopicToMap(topicName);
            discoveryData.getChannels().add(getChannelMap().get(topicName));
        }
    }

    public void addChannelTopicToMap(final String topic) {
        if (getChannelMap().containsKey(topic)) {
            return;
        }

        Channel channel = new Channel();
        channel.setId(getNextGeneratedId());
        channel.setName(topic);
        channel.setAdditionalAttributes(Map.of("type", "topic"));
        getChannelMap().put(channel.getName(), channel);
    }

    public Map<String, Channel> getChannelMap() {
        return channelMap;
    }

    public void addClientToSubscriptionRelationship(final String topicName, final Client client) {
        Channel channel = getChannelMap().get(topicName);
        if (channel == null || client == null) {
            return;
        }

        Subscription subscription = getOrCreateSubscription(channel);

        ClientToSubscriptionRelationship clientToSubscriptionRelationship = new ClientToSubscriptionRelationship();
        clientToSubscriptionRelationship.setClientId(client.getId());
        clientToSubscriptionRelationship.setSubscriptionId(subscription.getId());

        if (!getDiscoveryData().getClientToSubscriptionRelationships().contains(clientToSubscriptionRelationship)) {
            getDiscoveryData().getClientToSubscriptionRelationships().add(clientToSubscriptionRelationship);
        }
    }

    public void addClientToChannelRelationship(final String topicName, final Client client) {
        Channel channel = getChannelMap().get(topicName);
        if (channel == null || client == null) {
            return;
        }

        ClientToChannelRelationship clientToChannelRelationship = new ClientToChannelRelationship();
        clientToChannelRelationship.setClientId(client.getId());
        clientToChannelRelationship.setChannelId(channel.getId());

        if (!getDiscoveryData().getClientToChannelRelationships().contains(clientToChannelRelationship)) {
            getDiscoveryData().getClientToChannelRelationships().add(clientToChannelRelationship);
        }
    }

    private Subscription getOrCreateSubscription(final Channel topic) {
        return channelIdToSubscriptionMap.computeIfAbsent(topic.getId(), (key) -> {
            Subscription subscription = createSubscription(topic);
            getDiscoveryData().getSubscriptions().add(subscription);

            return subscription;
        });
    }

    private Subscription createSubscription(final Channel topic) {
        Subscription subscription = new Subscription();
        subscription.setId(getNextGeneratedId());
        subscription.setMatchCriteria(topic.getName());
        return subscription;
    }

    public boolean topicNameMatchesSubscriptionSet(String topicName) {
        if (discoveryOperation.getSubscriptionSet().size() > 0) {
            for (String subscriptionRegex : discoveryOperation.getSubscriptionSet()) {
                if (topicName.matches(subscriptionRegex)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private void mapTopicConfigToDiscoveryData(Topic discoveredTopic, Config config) {
        TopicConfig topicConfig = new TopicConfig();
        topicConfig.setRetentionMs(createTopicConfigNumberFromConfig(config.get("retention.ms")));
        topicConfig.setRetentionBytes(createTopicConfigIntegerFromConfig(config.get("retention.bytes")));
        topicConfig.setSegmentBytes(createTopicConfigIntegerFromConfig(config.get("segment.bytes")));
        topicConfig.setSegmentIndexBytes(createTopicConfigIntegerFromConfig(config.get("segment.index.bytes")));
        topicConfig.setSegmentMs(createTopicConfigNumberFromConfig(config.get("segment.ms")));
        topicConfig.setSegmentJitterMs(createTopicConfigNumberFromConfig(config.get("segment.jitter.ms")));
        topicConfig.setMinInsyncReplicas(createTopicConfigIntegerFromConfig(config.get("min.insync.replicas")));
        topicConfig.setMaxMessageBytes(createTopicConfigIntegerFromConfig(config.get("max.message.bytes")));
        topicConfig.setIndexIntervalBytes(createTopicConfigIntegerFromConfig(config.get("index.interval.bytes")));
        discoveredTopic.setTopicConfig(topicConfig);
    }

    private TopicConfigValuesInteger createTopicConfigIntegerFromConfig(ConfigEntry configEntry) {
        TopicConfigValuesInteger topicConfigInt = new TopicConfigValuesInteger();
        if (configEntry != null) {
            try {
                topicConfigInt.setValue(Integer.valueOf(configEntry.value()));
            } catch (NumberFormatException ex) {
                log.warn("Could not determine integer value of {} for {}",
                        configEntry.value(),
                        configEntry.name());
                topicConfigInt.setValue(-1);
            }
            topicConfigInt.setDefault(configEntry.isDefault());
        }
        return topicConfigInt;
    }

    private TopicConfigValuesNumber createTopicConfigNumberFromConfig(ConfigEntry configEntry) {
        TopicConfigValuesNumber topicConfigNum = new TopicConfigValuesNumber();
        if (configEntry != null) {
            try {
                topicConfigNum.setValue(Double.valueOf(configEntry.value()));
            } catch (NumberFormatException ex) {
                log.warn("Could not determine integer value of {} for {}",
                        configEntry.value(),
                        configEntry.name());
                topicConfigNum.setValue(-1D);
            }
            topicConfigNum.setDefault(configEntry.isDefault());
        }
        return topicConfigNum;
    }


    public List<String> getTopicList() throws InterruptedException, ExecutionException {
        return adminClient.listTopics().listings().get()
                .stream()
                .filter(tl -> !internalTopicName(tl.name()))
                .map(TopicListing::name)
                .collect(Collectors.toList());
    }

    public boolean internalTopicName(String topicName) {
        return topicName.startsWith("_") || topicName.startsWith("docker-connect-");
    }


    public List<String> getTopics() {
        return topics;
    }

    protected void discoveryKafkaAcls() throws InterruptedException, ExecutionException {
        final DescribeAclsResult describeAclsResult = adminClient.describeAcls(AclBindingFilter.ANY);
        try {
            kafkaConfigDiscoveryData.setKafkaAcl(getKafkaAcls(describeAclsResult.values().get()));
        } catch (final ExecutionException e) {
            if (e.getCause() instanceof SecurityDisabledException) {
                log.info("No Authorizer is configured on the broker");
            } else if (e.getCause() instanceof ClusterAuthorizationException) {
                log.info("User '" + brokerAuthentication.getAdminUsername() + "' is not authorized to discover ACLs");
            } else if (e.getCause() instanceof UnsupportedVersionException) {
                log.info("Ignoring ACLs." + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    protected List<KafkaAcl> getKafkaAcls(final Collection<AclBinding> aclBindings) {
        return aclBindings.stream()
                .filter(Objects::nonNull)
                .map(this::createKafkaAcl)
                .collect(Collectors.toList());
    }

    protected KafkaAcl createKafkaAcl(final AclBinding aclBinding) {
        final KafkaAcl kafkaAcl = new KafkaAcl();
        kafkaAcl.setEntry(createKafkaAclEntry(aclBinding.entry()));
        kafkaAcl.setPattern(createKafkaAclPattern(aclBinding.pattern()));
        return kafkaAcl;
    }

    protected KafkaAclEntry createKafkaAclEntry(final AccessControlEntry accessControlEntry) {
        final KafkaAclEntry kafkaAclEntry = new KafkaAclEntry();
        kafkaAclEntry.setPrincipal(accessControlEntry.principal());
        kafkaAclEntry.setHost(accessControlEntry.host());
        kafkaAclEntry.setPermissionType(accessControlEntry.permissionType().name());
        kafkaAclEntry.setOperation(accessControlEntry.operation().name());
        return kafkaAclEntry;
    }

    protected KafkaAclPattern createKafkaAclPattern(final org.apache.kafka.common.resource.ResourcePattern kafkaResourcePattern) {
        final ResourcePattern resourcePattern = new ResourcePattern();
        resourcePattern.setName(kafkaResourcePattern.name());
        resourcePattern.setPaternType(kafkaResourcePattern.patternType().name());
        resourcePattern.setResourceType(kafkaResourcePattern.resourceType().name());

        final KafkaAclPattern kafkaAclPattern = new KafkaAclPattern();
        kafkaAclPattern.setResourcePattern(resourcePattern);

        return kafkaAclPattern;
    }

    public void createConnectorClient() {
        kafkaConnectorUtils.createConnectorClient();
    }

    public AdminClient getAdminClient() {
        return adminClient;
    }

    public KafkaDiscoveryData getKafkaConfigDiscoveryData() {
        return kafkaConfigDiscoveryData;
    }

    public DiscoveryData getDiscoveryData() {
        return discoveryData;
    }

    public ApacheKafkaIdentity getBrokerIdentity() {
        return brokerIdentity;
    }

    public ApacheKafkaAuthentication getBrokerAuthentication() {
        return brokerAuthentication;
    }
}
