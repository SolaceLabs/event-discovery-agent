package com.event.discovery.agent.integrationTests.offline.kafkaCommon;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeAclsResult;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.MemberAssignment;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@Component
public class KafkaAdminClientMocks {
    @Autowired
    protected ObjectProvider<AdminClient> adminClientObjectProvider;

    @Autowired
    @Qualifier("testAdminClientProperties")
    protected Properties adminClientProperties;

    protected AclBinding binding;

    protected AdminClient adminClient;

    public void setupAdminClientMock() {
        adminClientProperties.clear();
        adminClient = adminClientObjectProvider.getObject(new Properties());
    }

    public void resetAdminClientMock() {
        reset(adminClientObjectProvider.getObject(new Properties()));
    }

    public void setupKafkaAdminClient() throws InterruptedException, ExecutionException {

        // Mock the cluster
        mockDescribeCluster(adminClient);

        // Setup mocks for AdminClient
        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        when(adminClient.listTopics()).thenReturn(listTopicsResult);
        KafkaFuture<Collection<TopicListing>> mockTopicListingFuture = mock(KafkaFuture.class);
        when(listTopicsResult.listings()).thenReturn(mockTopicListingFuture);

        TopicListing topicListing1 = new TopicListing("ATopic", false);
        Collection<TopicListing> topicListings = new ArrayList<>();
        topicListings.add(topicListing1);
        when(mockTopicListingFuture.get()).thenReturn(topicListings);

        ListConsumerGroupsResult listConsumerGroupsResult = mock(ListConsumerGroupsResult.class);
        when(adminClient.listConsumerGroups()).thenReturn(listConsumerGroupsResult);
        KafkaFuture listConsumerGroupsFuture = mock(KafkaFuture.class);
        ConsumerGroupListing consumerGroupListing = mock(ConsumerGroupListing.class);
        when(consumerGroupListing.groupId()).thenReturn("AConsumerGroup");
        ConsumerGroupListing consumerGroupListing2 = mock(ConsumerGroupListing.class);
        when(consumerGroupListing2.groupId()).thenReturn("BConsumerGroup");
        Collection<ConsumerGroupListing> consumerGroupListings = new ArrayList<>();
        consumerGroupListings.add(consumerGroupListing);
        consumerGroupListings.add(consumerGroupListing2);
        when(listConsumerGroupsFuture.get()).thenReturn(consumerGroupListings);
        when(listConsumerGroupsResult.all()).thenReturn(listConsumerGroupsFuture);

        mockConsumerGroups(new TopicPartition("ATopic", 0));

        DescribeConfigsResult describeConfigsResult = mock(DescribeConfigsResult.class);
        when(adminClient.describeConfigs(any())).thenReturn(describeConfigsResult);
        KafkaFuture kafkaFuture = mock(KafkaFuture.class);
        when(describeConfigsResult.all()).thenReturn(kafkaFuture);
        Map configMap = mock(Map.class);
        when(kafkaFuture.get()).thenReturn(configMap);
        Config entries = buildTopicConfigList();
        when(configMap.get(any())).thenReturn(entries);

        ListConsumerGroupOffsetsResult lcgo = mock(ListConsumerGroupOffsetsResult.class);
        KafkaFuture fkOffset = mock(KafkaFuture.class);
        when(adminClient.listConsumerGroupOffsets(any())).thenReturn(lcgo);
        when(lcgo.partitionsToOffsetAndMetadata()).thenReturn(fkOffset);
        when(fkOffset.get()).thenReturn(Collections.emptyMap());
    }

    public void mockConsumerGroups(TopicPartition topicPartition) throws InterruptedException, ExecutionException {
        DescribeConsumerGroupsResult describeConsumerGroupsResult = mock(DescribeConsumerGroupsResult.class);
        when(adminClient.describeConsumerGroups(any())).thenReturn(describeConsumerGroupsResult);
        KafkaFuture describeConsumerGroupsFuture = mock(KafkaFuture.class);
        when(describeConsumerGroupsResult.all()).thenReturn(describeConsumerGroupsFuture);
        ConsumerGroupDescription aConsumerGroupDescription = mock(ConsumerGroupDescription.class);
        MemberAssignment memberAssignment = new MemberAssignment(Collections.singleton(topicPartition));
        MemberDescription memberDescription = new MemberDescription("memberId", "clientId", "hostname", memberAssignment);
        Collection<MemberDescription> members = Arrays.asList(memberDescription);
        when(aConsumerGroupDescription.members()).thenReturn(members);
        when(aConsumerGroupDescription.groupId()).thenReturn("AConsumerGroup");

        ConsumerGroupDescription bConsumerGroupDescription = mock(ConsumerGroupDescription.class);
        when(bConsumerGroupDescription.members()).thenReturn(Collections.emptyList());
        when(bConsumerGroupDescription.groupId()).thenReturn("BConsumerGroup");

        Map<String, ConsumerGroupDescription> groups = new HashMap<>();
        groups.put("AConsumerGroup", aConsumerGroupDescription);
        groups.put("BConsumerGroup", bConsumerGroupDescription);
        when(describeConsumerGroupsFuture.get()).thenReturn(groups);
    }

    private Config buildTopicConfigList() {
        List<ConfigEntry> configEntries = new ArrayList<>();
        configEntries.add(new ConfigEntry("retention.ms", "1"));
        configEntries.add(new ConfigEntry("retention.bytes", "2"));
        configEntries.add(new ConfigEntry("segment.bytes", "3"));
        configEntries.add(new ConfigEntry("segment.index.bytes", "4"));
        configEntries.add(new ConfigEntry("segment.ms", "5"));
        configEntries.add(new ConfigEntry("segment.jitter.ms", "6"));
        configEntries.add(new ConfigEntry("min.insync.replicas", "7"));
        configEntries.add(new ConfigEntry("max.message.bytes", "8"));
        configEntries.add(new ConfigEntry("index.interval.bytes", "9"));
        return new Config(configEntries);
    }

    private void mockDescribeCluster(AdminClient adminClient) throws InterruptedException, ExecutionException {
        // Mocks for cluster information
        DescribeClusterResult cluster = mock(DescribeClusterResult.class);
        when(adminClient.describeCluster()).thenReturn(cluster);
        KafkaFuture<String> clusterIdFuture = mock(KafkaFuture.class);
        when(cluster.clusterId()).thenReturn(clusterIdFuture);
        when(clusterIdFuture.get()).thenReturn("ThisIsMyClusterId");

        KafkaFuture<Collection<Node>> nodesFuture = mock(KafkaFuture.class);
        when(cluster.nodes()).thenReturn(nodesFuture);
        Node firstNode = new Node(1, "FirstNode", 90210);
        Node secondNode = new Node(2, "NodeDeux", 90211);
        Node thirdNode = new Node(2, "NodeTres", 90212);

        Collection<Node> clusterNodes = List.of(firstNode, secondNode, thirdNode);
        when(nodesFuture.get()).thenReturn(clusterNodes);
    }

    public void configureDescribeAclResult(final KafkaFuture<Collection<AclBinding>> aclBindings) {
        try {
            // DescribeAclsResult doesn't have a public constructor
            Constructor<DescribeAclsResult> constructor = (Constructor<DescribeAclsResult>) DescribeAclsResult.class.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            DescribeAclsResult describeAclsResult = constructor.newInstance(aclBindings);
            when(adminClient.describeAcls(any(AclBindingFilter.class))).thenReturn(describeAclsResult);
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public void setUpDefaultAcls() {
        ResourcePattern kafkaResourcePattern = new ResourcePattern(ResourceType.TOPIC, "AclTopic", PatternType.LITERAL);
        AccessControlEntry accessControlEntry = new AccessControlEntry("User:*", "*", AclOperation.ALTER, AclPermissionType.ALLOW);
        List<AclBinding> bindings = Stream.of(new AclBinding(kafkaResourcePattern, accessControlEntry)).collect(Collectors.toList());
        KafkaFuture<Collection<AclBinding>> future = KafkaFuture.completedFuture(bindings);

        configureDescribeAclResult(future);
        binding = bindings.get(0);
    }

    public void setupInactiveConsumer() throws ExecutionException, InterruptedException {
        KafkaFuture fkOffset = setupListTopicsAndListConsumerGroups();

        Map<TopicPartition, OffsetAndMetadata> offsetAndMetadataMap = new HashMap<>();
        TopicPartition topicPartition = new TopicPartition("BTopic", 3);
        OffsetAndMetadata offsetAndMetadata = new OffsetAndMetadata(444, Optional.of(1), "I'm so meta");
        offsetAndMetadataMap.put(topicPartition, offsetAndMetadata);

        // Build an offset map for BTopic
        when(fkOffset.get()).thenReturn(offsetAndMetadataMap);
    }

    private KafkaFuture setupListTopicsAndListConsumerGroups() throws InterruptedException, ExecutionException {
        // Return 2 topics ATopic, BTopic
        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        when(adminClient.listTopics()).thenReturn(listTopicsResult);
        KafkaFuture<Collection<TopicListing>> mockTopicListingFuture;
        mockTopicListingFuture = mock(KafkaFuture.class);
        when(listTopicsResult.listings()).thenReturn(mockTopicListingFuture);

        TopicListing topicListing1 = new TopicListing("ATopic", false);
        TopicListing topicListing2 = new TopicListing("BTopic", false);
        Collection<TopicListing> topicListings = new ArrayList<>();
        topicListings.add(topicListing1);
        topicListings.add(topicListing2);
        when(mockTopicListingFuture.get()).thenReturn(topicListings);

        ListConsumerGroupOffsetsResult lcgo = mock(ListConsumerGroupOffsetsResult.class);
        KafkaFuture fkOffset = mock(KafkaFuture.class);
        when(adminClient.listConsumerGroupOffsets("BConsumerGroup")).thenReturn(lcgo);
        when(lcgo.partitionsToOffsetAndMetadata()).thenReturn(fkOffset);
        return fkOffset;
    }

    public void setupExceptionForInactiveConsumer() throws ExecutionException, InterruptedException {

        // Return 2 topics ATopic, BTopic
        KafkaFuture fkOffset = setupListTopicsAndListConsumerGroups();

        // Build an offset map for BTopic
        when(fkOffset.get()).thenThrow(new ExecutionException("Stuff broker", new IllegalArgumentException()));
    }


    public ObjectProvider<AdminClient> getAdminClientObjectProvider() {
        return adminClientObjectProvider;
    }

    public AdminClient getAdminClient() {
        return adminClient;
    }

    public AclBinding getBinding() {
        return binding;
    }

    public Properties getAdminClientProperties() {
        return adminClientProperties;
    }
}
