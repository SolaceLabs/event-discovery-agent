package com.event.discovery.agent.kafkaCommon;

import com.event.discovery.agent.types.config.ConsumerGroup;
import com.event.discovery.agent.types.config.ConsumerGroupAssignment;
import com.event.discovery.agent.types.config.ConsumerGroupMembers;
import com.event.discovery.agent.types.config.ConsumerGroupOffset;
import com.event.discovery.agent.types.config.Topic;
import com.event.discovery.agent.types.event.common.Client;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.MemberAssignment;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.event.discovery.agent.framework.model.CommonModelConstants.Client.CLIENT_TYPE_APPLICATION;
import static com.event.discovery.agent.framework.model.CommonModelConstants.Client.CLIENT_TYPE_KEY;
import static com.event.discovery.agent.framework.model.CommonModelConstants.Client.SIMPLE_CONSUMER;
import static com.event.discovery.agent.framework.model.CommonModelConstants.Client.TYPE_CONSUMER;

@Slf4j
public class KafkaConsumerGroupUtils {
    private final KafkaCommon kafkaCommon;
    private final Map<String, Client> consumerGroupIdToClient = new HashMap<>();

    public KafkaConsumerGroupUtils(KafkaCommon kafkaCommon) {
        this.kafkaCommon = kafkaCommon;
    }

    public void discoveryKafkaConsumerGroups() throws InterruptedException, ExecutionException {
        // Get the list of Consumer Group Ids
        Map<String, ConsumerGroupListing> kafkaConsumerGroups = kafkaCommon.getAdminClient().listConsumerGroups().all().get()
                .stream()
                .collect(Collectors.toMap(ConsumerGroupListing::groupId, x -> x));

        // Get the Description Map for each of the Consumer Groups
        Map<String, ConsumerGroupDescription> consumerGroupDescriptionMap =
                kafkaCommon.getAdminClient().describeConsumerGroups(kafkaConsumerGroups.keySet()).all().get();

        // Loop of the list of Consumer groups and map them to our own Consumer Group Object
        List<ConsumerGroup> consumerGroupList = consumerGroupDescriptionMap.keySet().stream()
                .filter(this::allowConsumerGroup)
                .map(cgId -> getConfigConsumerGroup(kafkaConsumerGroups, consumerGroupDescriptionMap, cgId))
                .collect(Collectors.toList());
        kafkaCommon.getKafkaConfigDiscoveryData().setKafkaConsumerGroups(consumerGroupList);

        List<Client> consumerGroupClients = new ArrayList<>();

        consumerGroupDescriptionMap.keySet().stream()
                .filter(this::allowConsumerGroup)
                .forEach(cgId -> {
                    Client cgClient = getConsumerGroupClient(kafkaConsumerGroups, cgId);
                    consumerGroupIdToClient.put(cgClient.getName(), cgClient);
                    consumerGroupClients.add(cgClient);
                });

        kafkaCommon.getDiscoveryData().getClients().addAll(consumerGroupClients);
    }

    private boolean allowConsumerGroup(String consumerGroupName) {
        return !(consumerGroupName.startsWith("_") || consumerGroupName.startsWith("EventListeningConsumer")
                || consumerGroupName.equals(""));
    }

    private Client getConsumerGroupClient(
            Map<String, ConsumerGroupListing> kafkaConsumerGroups, String cgId) {
        Client cgClient = new Client();
        cgClient.setId(kafkaCommon.getNextGeneratedId());
        cgClient.setName(cgId);
        cgClient.setType(TYPE_CONSUMER);
        cgClient.setAdditionalAttributes(Map.of(
                CLIENT_TYPE_KEY, CLIENT_TYPE_APPLICATION,
                SIMPLE_CONSUMER, kafkaConsumerGroups.get(cgId).isSimpleConsumerGroup()
        ));
        return cgClient;
    }

    private ConsumerGroup getConfigConsumerGroup(Map<String, ConsumerGroupListing> kafkaConsumerGroups,
                                                 Map<String, ConsumerGroupDescription> consumerGroupDescriptionMap, String cgId) {
        ConsumerGroup cg = new ConsumerGroup();
        ConsumerGroupDescription cgDescription = consumerGroupDescriptionMap.get(cgId);
        Collection<MemberDescription> memberDescriptions = cgDescription.members();
        List<ConsumerGroupMembers> consumerGroupMembersList = memberDescriptions.stream()
                .map(this::getConsumerGroupMembers)
                .collect(Collectors.toList());
        cg.setGroupId(cgId);
        cg.setMembers(consumerGroupMembersList);
        cg.setIsSimpleConsumerGroup(kafkaConsumerGroups.get(cgId).isSimpleConsumerGroup());
        getConsumerGroupOffsets(cg);
        return cg;
    }

    private void getConsumerGroupOffsets(ConsumerGroup cg) {
        try {
            String cgId = cg.getGroupId();
            ListConsumerGroupOffsetsResult lcgo = kafkaCommon.getAdminClient().listConsumerGroupOffsets(cgId);
            KafkaFuture<Map<TopicPartition, OffsetAndMetadata>> cgOffsets = lcgo.partitionsToOffsetAndMetadata();
            Map<TopicPartition, OffsetAndMetadata> cgOffsetMap;
            try {
                cgOffsetMap = cgOffsets.get();
            } catch (InterruptedException | ExecutionException e) {
                // Silently eat the exception for now.
                // Eventually add it to the warnings list for the discovery
                return;
            }

            List<ConsumerGroupOffset> offsets = new ArrayList<>();
            cgOffsetMap.forEach((k, v) -> {
                ConsumerGroupOffset cgOffset = new ConsumerGroupOffset();
                cgOffset.setTopic(k.topic());
                cgOffset.setPartition(k.partition());
                cgOffset.setOffset(Long.toString(v.offset()));
                cgOffset.setLeaderEpoch(v.leaderEpoch().orElse(0));
                cgOffset.setMetadata(v.metadata());
                offsets.add(cgOffset);
            });
            cg.setOffsets(offsets);
        } catch (RuntimeException e) {
            // Silently eat the exception for now.
            // Eventually add it to the warnings list for the discovery
        }
    }

    private ConsumerGroupMembers getConsumerGroupMembers(MemberDescription memberDescription) {
        ConsumerGroupMembers consumerGroupMembers = new ConsumerGroupMembers();
        consumerGroupMembers.setGroupInstanceId(memberDescription.consumerId());
        consumerGroupMembers.setClientId(memberDescription.clientId());
        consumerGroupMembers.setHost(memberDescription.host());
        MemberAssignment memberAssignment = memberDescription.assignment();

        Map<String, com.event.discovery.agent.types.config.TopicPartition> partitions = new HashMap<>();
        memberAssignment.topicPartitions().stream()
                .forEach(tp -> {
                    String topic = tp.topic();
                    if (partitions.containsKey(topic)) {
                        com.event.discovery.agent.types.config.TopicPartition topicPartition = partitions.get(topic);
                        topicPartition.getPartitions().add(tp.partition());
                    } else {
                        com.event.discovery.agent.types.config.TopicPartition topicPartition =
                                new com.event.discovery.agent.types.config.TopicPartition();
                        topicPartition.setTopic(topic);
                        topicPartition.setPartitions(new ArrayList<>());
                        topicPartition.getPartitions().add(tp.partition());
                        partitions.put(topic, topicPartition);
                    }
                });
        ConsumerGroupAssignment consumerGroupAssignment = new ConsumerGroupAssignment();
        consumerGroupAssignment.setTopicPartitions(new ArrayList<>(partitions.values()));
        consumerGroupMembers.setAssignment(consumerGroupAssignment);
        return consumerGroupMembers;
    }

    public void getConsumerSubscriptions() {

        // Find all consumer groups that match the topic
        kafkaCommon.getKafkaConfigDiscoveryData().getTopic().stream()
                .filter(topic -> topic != null && topic.getTopicName() != null)
                .filter(topic -> !kafkaCommon.internalTopicName(topic.getTopicName()))
                .map(Topic::getTopicName)
                .forEach(topicName -> {
                    kafkaCommon.getTopics().add(topicName);
                    kafkaCommon.addChannelTopicToMap(topicName);
                    getConsumerGroupsForTopicName(topicName);
                    // log.info(String.format("getConsumerSubscriptions() -- #### MAPPED TOPIC NAME == %s ####", topicName));
                });

    }

    private void getConsumerGroupsForTopicName(String topicName) {
        kafkaCommon.getKafkaConfigDiscoveryData().getKafkaConsumerGroups().stream()
                .forEach(cg -> {
                    // Check active consumer
                    processActiveConsumer(topicName, cg);
                    // Check Inactive consumers
                    processInactiveConsumer(topicName, cg);
                    // log.info(String.format("getConsumerGroupsForTopicName() -- #### MAPPED TOPIC NAME == %s ####", topicName));
                });
    }

    private void processInactiveConsumer(String topicName, ConsumerGroup cg) {
        if (!CollectionUtils.isEmpty(cg.getOffsets())) {
            for (ConsumerGroupOffset cgOffset : cg.getOffsets()) {
                if (topicName.equals(cgOffset.getTopic())) {
                    addConsumerGroupToSubscriptionRelationship(topicName, cg.getGroupId());
                    // log.info(String.format("processInactiveConsumer() -- #### MAPPED TOPIC NAME == %s ####", topicName));
                }
            }
        }
    }

    private void processActiveConsumer(String topicName, ConsumerGroup cg) {
        if (!CollectionUtils.isEmpty(cg.getMembers())) {
            for (ConsumerGroupMembers member : cg.getMembers()) {
                if (member.getAssignment() != null && !CollectionUtils.isEmpty(member.getAssignment().getTopicPartitions())) {
                    for (com.event.discovery.agent.types.config.TopicPartition partition : member.getAssignment().getTopicPartitions()) {
                        if (topicName.equals(partition.getTopic())) {
                            addConsumerGroupToSubscriptionRelationship(topicName, cg.getGroupId());
                            // log.info(String.format("processActiveConsumer() -- #### MAPPED TOPIC NAME == %s ####", topicName));
                        }
                    }
                }
            }
        }
    }

    private void addConsumerGroupToSubscriptionRelationship(String topicName, String cgId) {
        Client consumerGroup = consumerGroupIdToClient.get(cgId);

        kafkaCommon.addClientToSubscriptionRelationship(topicName, consumerGroup);
    }
}
