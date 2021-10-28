package com.event.discovery.agent.google.api;

import com.event.discovery.agent.google.GooglePubSubSubscriber;
import com.event.discovery.agent.google.factories.SubscriptionAdminClientFactory;
import com.event.discovery.agent.google.factories.TopicAdminClientFactory;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.cloud.pubsub.v1.stub.PublisherStubSettings;
import com.google.pubsub.v1.DeleteSubscriptionRequest;
import com.google.pubsub.v1.GetSubscriptionRequest;
import com.google.pubsub.v1.ListSubscriptionsRequest;
import com.google.pubsub.v1.ListTopicsRequest;
import com.google.pubsub.v1.ProjectName;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import com.event.discovery.agent.framework.AbstractBrokerPlugin;
import com.event.discovery.agent.framework.exception.DiscoverySupportCode;
import com.event.discovery.agent.framework.exception.EventDiscoveryAgentException;
import com.event.discovery.agent.google.GooglePubSubUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GooglePubSubApiImpl implements GooglePubSubApi {
    private static final String DISCOVERY_SUBSCRIPTION_PREFIX = "solace-discovery-";
    private TopicAdminClient topicAdminClient;
    private SubscriptionAdminClient subscriptionAdminClient;
    private String projectId;
    private ProjectName projectName;
    private final ObjectProvider<TopicAdminClientFactory> topicAdminClientFactoryObjectProvider;
    private final ObjectProvider<SubscriptionAdminClientFactory> subscriptionAdminClientFactoryObjectProvider;
    private final ObjectProvider<GooglePubSubSubscriber> googlePubSubSubscriptionObjectProvider;

    @Autowired
    public GooglePubSubApiImpl(ObjectProvider<TopicAdminClientFactory> topicAdminClientFactoryObjectProvider,
                               ObjectProvider<SubscriptionAdminClientFactory> subscriptionAdminClientFactoryObjectProvider,
                               ObjectProvider<GooglePubSubSubscriber> googlePubSubSubscriptionObjectProvider) {
        this.topicAdminClientFactoryObjectProvider = topicAdminClientFactoryObjectProvider;
        this.subscriptionAdminClientFactoryObjectProvider = subscriptionAdminClientFactoryObjectProvider;
        this.googlePubSubSubscriptionObjectProvider = googlePubSubSubscriptionObjectProvider;
    }

    @Override
    public void configure(String projectId, String credentials) {
        this.projectId = projectId;
        projectName = ProjectName.of(projectId);

        try {
            CredentialsProvider credentialsProvider = FixedCredentialsProvider.create(
                    ServiceAccountCredentials.fromStream(new ByteArrayInputStream(credentials.getBytes(StandardCharsets.UTF_8))));
            PublisherStubSettings publisherStubSettings = PublisherStubSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
            TopicAdminSettings topicAdminSettings = TopicAdminSettings.create(publisherStubSettings);
            topicAdminClient = topicAdminClientFactoryObjectProvider.getObject().getTopicAdminClient(topicAdminSettings);

            SubscriptionAdminSettings subscriptionAdminSettings =
                    SubscriptionAdminSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
            subscriptionAdminClient = subscriptionAdminClientFactoryObjectProvider.getObject()
                    .getSubscriptionAdminClient(subscriptionAdminSettings);
        } catch (IOException e) {
            log.error("Could not create clients", e);
            throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101, "Could not create GCP Clients", e);
        }
    }

    @Override
    public List<Topic> getTopicList() {
        List<Topic> returnTopicList = new ArrayList<>();
        String nextPageToken = "";

        do {
            ListTopicsRequest listTopicsRequest = ListTopicsRequest.newBuilder()
                    .setProject(projectName.toString())
                    .setPageToken(nextPageToken)
                    .build();

            TopicAdminClient.ListTopicsPagedResponse topicList;
            topicList = topicAdminClient.listTopics(listTopicsRequest);
            returnTopicList.addAll(topicList.getPage().getResponse().getTopicsList());
            nextPageToken = topicList.getNextPageToken();
        } while (!nextPageToken.equals(""));

        return returnTopicList;
    }

    @Override
    public List<GooglePubSubSubscriber> bindMessageHandlers(List<Subscription> subscriptionList,
                                                            AbstractBrokerPlugin brokerPlugin) {
        List<GooglePubSubSubscriber> messageReceiverList = new ArrayList<>();
        for (Subscription subscription : subscriptionList) {
            String topicName = GooglePubSubUtils.getResourceNameFromResourcePath(subscription.getTopic());
            GooglePubSubSubscriber messageReceiver = googlePubSubSubscriptionObjectProvider.getObject();
            messageReceiver.configure(topicName, brokerPlugin);
            messageReceiver.bindReceiver(projectId,
                    GooglePubSubUtils.getResourceNameFromResourcePath(subscription.getName()));
            messageReceiverList.add(messageReceiver);
        }
        return messageReceiverList;
    }

    @Override
    public void clearSolaceDiscoveryAgentSubscriptions() {
        List<Subscription> subList = getSubscriptionList();
        for (Subscription sub : subList) {
            removeSolaceDiscoveryAgentSubscription(sub);
        }
    }

    @Override
    public void removeSolaceDiscoveryAgentSubscription(Subscription subscription) {
        String subName = subscription.getName();
        if (subName.contains(DISCOVERY_SUBSCRIPTION_PREFIX)) {
            log.info("Removing Solace subscription {}", subName);
            DeleteSubscriptionRequest deleteSubscriptionRequest = DeleteSubscriptionRequest.newBuilder()
                    .setSubscription(subName).build();
            subscriptionAdminClient.deleteSubscription(deleteSubscriptionRequest);
        }
    }

    private List<Subscription> createSolaceDiscoveryAgentSubscriptions(List<Topic> topicList) {
        List<Subscription> subscriptionList = new ArrayList<>();

        for (Topic topic : topicList) {
            log.info("Adding subscription for topic: {}", topic.getName());
            Subscription subscription = getOrCreateSolaceDiscoveryAgentSubscription(topic);
            if (subscription != null) {
                subscriptionList.add(subscription);
            }
        }
        return subscriptionList;
    }

    @Override
    public List<Subscription> createSolaceDiscoveryAgentSubscriptions(Set<String> topicNameSet) {
        List<Topic> topicList = getTopicListForTopicNameSet(topicNameSet);
        return createSolaceDiscoveryAgentSubscriptions(topicList);
    }

    private List<Topic> getTopicListForTopicNameSet(Set<String> topicNameSet) {
        List<Topic> topicList = new ArrayList<>();
        for (String topicName : topicNameSet) {
            // They deprecated this without giving an alternative (https://github.com/googleapis/java-pubsub/issues/58)
            ProjectTopicName projectTopicName = ProjectTopicName.of(projectId, topicName);
            Topic topic = getTopic(projectTopicName.toString());
            if (topic != null) {
                topicList.add(topic);
            }
        }
        return topicList;
    }

    private Topic getTopic(String fullyQualifiedTopicName) {
        return topicAdminClient.getTopic(fullyQualifiedTopicName);
    }

    @Override
    public List<Subscription> getSubscriptionList() {
        List<Subscription> subscriptionList = new ArrayList<>();
        String nextPageToken = "";

        do {
            ListSubscriptionsRequest request = ListSubscriptionsRequest.newBuilder()
                    .setProject(projectName.toString())
                    .setPageToken(nextPageToken)
                    .build();

            SubscriptionAdminClient.ListSubscriptionsPagedResponse listSubscriptionsPagedResponse
                    = subscriptionAdminClient.listSubscriptions(request);
            subscriptionList.addAll(listSubscriptionsPagedResponse.getPage().getResponse().getSubscriptionsList());
            nextPageToken = listSubscriptionsPagedResponse.getNextPageToken();
        } while (!nextPageToken.equals(""));
        return subscriptionList;
    }

    private Subscription getOrCreateSolaceDiscoveryAgentSubscription(Topic topic) {
        String fullTopicName = topic.getName();
        Path path = Paths.get(fullTopicName);
        Path lastPartOfTopicName = path.getFileName();

        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(
                projectId, DISCOVERY_SUBSCRIPTION_PREFIX + GooglePubSubUtils.getResourceNameFromResourcePath(topic.getName()));
        ProjectTopicName projectTopicName = ProjectTopicName.newBuilder()
                .setProject(projectId)
                .setTopic(lastPartOfTopicName.toString())
                .build();

        Subscription subscription = null;
        try {
            subscription =
                    subscriptionAdminClient.createSubscription(
                            subscriptionName,
                            projectTopicName,
                            PushConfig.getDefaultInstance(),
                            0);
        } catch (AlreadyExistsException e) {
            log.info("Ignoring already exists exceptions");

            GetSubscriptionRequest subscriptionRequest = GetSubscriptionRequest.newBuilder()
                    .setSubscription(subscriptionName.toString())
                    .build();
            subscription = subscriptionAdminClient.getSubscription(subscriptionRequest);
        }

        log.info("Subscription {}:{} created.\n", subscriptionName.getProject(), subscriptionName.getSubscription());
        return subscription;
    }

    @Override
    public void cleanUp() {
        subscriptionAdminClient.close();
        topicAdminClient.close();
    }
}
