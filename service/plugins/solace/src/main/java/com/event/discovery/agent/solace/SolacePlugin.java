package com.event.discovery.agent.solace;

import com.event.discovery.agent.types.event.common.DiscoveryData;
import com.event.discovery.agent.framework.AbstractBrokerPlugin;
import com.event.discovery.agent.framework.VendorBrokerPlugin;
import com.event.discovery.agent.framework.exception.BrokerExceptionHandler;
import com.event.discovery.agent.framework.exception.DiscoverySupportCode;
import com.event.discovery.agent.framework.exception.EventDiscoveryAgentException;
import com.event.discovery.agent.framework.model.BasicPluginConfigData;
import com.event.discovery.agent.framework.model.BrokerConfigData;
import com.event.discovery.agent.framework.model.DiscoveryOperation;
import com.event.discovery.agent.framework.model.NormalizedEvent;
import com.event.discovery.agent.framework.model.PluginConfigData;
import com.event.discovery.agent.framework.model.broker.BrokerAuthentication;
import com.event.discovery.agent.framework.model.broker.BrokerCapabilities;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;
import com.event.discovery.agent.framework.model.messages.BasicNormalizedEvent;
import com.event.discovery.agent.framework.model.messages.BasicNormalizedMessage;
import com.event.discovery.agent.solace.mapper.SolaceToCommonModelMapper;
import com.event.discovery.agent.solace.model.AclProfile;
import com.event.discovery.agent.solace.model.Channel;
import com.event.discovery.agent.solace.model.ChannelType;
import com.event.discovery.agent.solace.model.Client;
import com.event.discovery.agent.solace.model.ClientProfile;
import com.event.discovery.agent.solace.model.ClientUsername;
import com.event.discovery.agent.solace.model.Queue;
import com.event.discovery.agent.solace.model.Subscription;
import com.event.discovery.agent.solace.model.TopicEndpoint;
import com.event.discovery.agent.solace.model.broker.SolaceBrokerAuthentication;
import com.event.discovery.agent.solace.model.broker.SolaceBrokerIdentity;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.impl.QueueImpl;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.Charsets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
public class SolacePlugin extends AbstractBrokerPlugin implements VendorBrokerPlugin {

    private final static String PLUGIN_NAME = "SOLACE";

    private final SolaceHttpSemp solaceHttpSemp;
    private final SolaceMessageConsumer solaceMessageConsumer;
    private final SolaceToCommonModelMapper solaceToCommonModelMapper;

    private final Map<String, Channel> channels = new HashMap<>();
    private final Map<String, ClientUsername> clientUsernames = new HashMap<>();
    private final Map<String, Client> clientNames = new HashMap<>();
    private final Map<String, AclProfile> aclProfiles = new HashMap<>();
    private final Map<String, ClientProfile> clientProfiles = new HashMap<>();
    private final Map<String, Subscription> subscriptions = new HashMap<>();

    @Autowired
    public SolacePlugin(MeterRegistry meterRegistry,
                        SolaceMessageConsumer solaceMessageConsumer,
                        SolaceHttpSemp solaceHttpSemp,
                        SolaceToCommonModelMapper solaceToCommonModelMapper) {
        super(meterRegistry);
        this.solaceMessageConsumer = solaceMessageConsumer;
        this.solaceHttpSemp = solaceHttpSemp;
        this.solaceToCommonModelMapper = solaceToCommonModelMapper;
    }

    @Override
    public void initialize(BrokerIdentity brokerIdentity,
                           BrokerAuthentication brokerAuthentication,
                           DiscoveryOperation discoveryOperation) {
        validateInput(discoveryOperation, (SolaceBrokerAuthentication) brokerAuthentication);
        super.initialize(brokerIdentity, brokerAuthentication, discoveryOperation);
        solaceHttpSemp.initialize((SolaceBrokerIdentity) brokerIdentity,
                (SolaceBrokerAuthentication) brokerAuthentication);
    }

    private void validateInput(DiscoveryOperation discoveryOperation, SolaceBrokerAuthentication brokerAuthentication) {
        if (discoveryOperation.getMessageQueueLength() < 1) {
            throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101,
                    "message queue length must be > 0");
        }
        if (CollectionUtils.isEmpty(discoveryOperation.getSubscriptionSet())) {
            throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101,
                    "subscription set cannot be empty");
        }
        if (discoveryOperation.getDurationInSecs() < 1) {
            throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101,
                    "discovery duration must be > 0");
        }
        if (brokerAuthentication.getTrustStoreLocation() != null) {
            if (!(new File(brokerAuthentication.getTrustStoreLocation()).isFile())) {
                throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101,
                        "file at trustStoreLocation does not exist");
            }
        }
    }

    @Override
    public BrokerConfigData getBrokerConfig() {
        try {
            solaceHttpSemp.discoverSempVersion();
            getClients();
            getQueueSubscriptions();
            getDTEsAndSubscriptions();
        } catch (IOException e) {
            log.error("Failed to get broker configuration", e);
        }
        DiscoveryData discoveryData = solaceToCommonModelMapper.map((SolaceBrokerIdentity) getBrokerIdentity(),
                channels.values(), clientNames.values(), subscriptions.values());
        return BrokerConfigData.builder()
                .configuration(Collections.EMPTY_MAP)
                .commonDiscoveryData(discoveryData)
                .build();
    }

    private void getClients() throws IOException {
        getClientUsernames();
        getClientsAndSubscriptions();
    }

    private void getDTEsAndSubscriptions() throws IOException {
        List<Map<String, Object>> dtesResponse = solaceHttpSemp.getDTEs();
        for (Map<String, Object> dte : dtesResponse) {
            boolean exclusive = "exclusive".equals(dte.get("accessType"));
            Boolean durable = (Boolean) getNonBackwardCompatableSempValue("2.11", dte, "durable", "isDurable");
            String destinationTopic =
                    (String) getNonBackwardCompatableSempValue("2.11", dte, "destinationTopic", "destination");
            String topicEndpointName = (String) dte.get("topicEndpointName");

            TopicEndpoint tEndpoint = TopicEndpoint.topicEndpointBuilder()
                    .subscriptions(new ArrayList<>())
                    .exclusive(exclusive)
                    .durable(durable)
                    .name(topicEndpointName)
                    .type(ChannelType.TOPIC_ENDPOINT)
                    .build();
            channels.put("TE-" + topicEndpointName, tEndpoint);

            if (!StringUtils.isEmpty(destinationTopic)) {
                addSubscriptionIfNotPresent(destinationTopic);
                tEndpoint.getSubscriptions().add(subscriptions.get(destinationTopic));
            }

            // Get txFlows
            List<Map<String, Object>> txFlows = solaceHttpSemp.getDTEsTxFlows(topicEndpointName);
            for (Map<String, Object> txFlow : txFlows) {
                String clientName = (String) txFlow.get("clientName");
                if (!StringUtils.isEmpty(clientName)) {
                    if (clientNames.containsKey(clientName)) {
                        Client clientObj = clientNames.get(clientName);
                        clientObj.getRxEndpoints().add(tEndpoint);
                    }
                }
            }
        }
    }

    private Object getNonBackwardCompatableSempValue(String lastOldVersion, Map<String, Object> responseMap,
                                                     String newAttributeName, String oldAttributeName) {
        if (solaceHttpSemp.sempVersionIsGreaterThan(lastOldVersion)) {
            return responseMap.get(newAttributeName);
        }
        return responseMap.get(oldAttributeName);
    }

    private void getClientsAndSubscriptions() throws IOException {
        List<Map<String, Object>> clientNamesResponse = solaceHttpSemp.getClients();
        for (Map<String, Object> client : clientNamesResponse) {
            String clientName = (String) client.get("clientName");
            if (!clientName.startsWith("#")) {

                String clientUsername = (String) client.get("clientUsername");
                String aclProfileName =
                        (String) getNonBackwardCompatableSempValue("2.12", client, "aclProfileName", "aclProfile");
                if (aclProfileName == null) {
                    aclProfileName = clientUsernames.get(clientUsername).getAclProfile().getName();
                }
                addAclProfileNotPresent(aclProfileName);

                String clientProfileName;
                if (solaceHttpSemp.sempVersionIsGreaterThan("2.12.0")) {
                    clientProfileName = (String) client.get("clientProfileName");
                } else if (solaceHttpSemp.sempVersionIsGreaterThan("2.10.0")) {
                    clientProfileName = (String) client.get("clientProfile");
                } else {
                    clientProfileName = (String) client.get("client");
                }

                if (clientProfileName == null) {
                    clientProfileName = clientUsernames.get(clientUsername).getClientProfile().getName();
                }
                addClientProfileIfNotPresent(clientProfileName);
                if (!clientName.startsWith("#")) {
                    Client clientObj = Client.builder()
                            .clientUsername(clientUsernames.get(clientUsername))
                            .name(clientName)
                            .aclProfile(aclProfiles.get(aclProfileName))
                            .clientProfile(clientProfiles.get(clientProfileName))
                            .rxEndpoints(new HashSet<>())
                            .rxSubscriptions(new HashSet<>())
                            .txDestination(new HashSet<>())
                            .build();
                    clientNames.put(clientName, clientObj);

                    getClientSubscriptions(clientName, clientObj);
                }
            }
        }
    }

    private void getClientSubscriptions(String clientName, Client clientObj) throws IOException {
        List<Map<String, Object>> subscriptionResponse = solaceHttpSemp.getClientSubscriptions(clientName);
        for (Map<String, Object> subscription : subscriptionResponse) {
            String subscriptionName;
            if (solaceHttpSemp.sempVersionIsGreaterThan("2.11")) {
                subscriptionName = (String) subscription.get("subscriptionTopic");
            } else {
                subscriptionName = (String) subscription.get("clientSubscription");
            }
            addSubscriptionIfNotPresent(subscriptionName);
            clientObj.getRxSubscriptions().add(subscriptions.get(subscriptionName));
        }
    }

    private void getClientUsernames() throws IOException {
        List<Map<String, Object>> clientUsernamesResponse = solaceHttpSemp.getClientUsernames();

        clientUsernamesResponse.forEach(
                cuMap -> {
                    String name = (String) cuMap.get("clientUsername");
                    if (!name.startsWith("#")) {
                        String aclProfileName = (String) cuMap.get("aclProfileName");
                        addAclProfileNotPresent(aclProfileName);
                        String clientProfileName = (String) cuMap.get("clientProfileName");
                        addClientProfileIfNotPresent(clientProfileName);
                        ClientUsername clientUsername = ClientUsername.builder()
                                .name(name)
                                .clients(new ArrayList<>())
                                .aclProfile(aclProfiles.get(aclProfileName))
                                .clientProfile(clientProfiles.get(clientProfileName))
                                .build();
                        clientUsernames.put(clientUsername.getName(), clientUsername);
                    }
                }
        );
    }

    private void addClientProfileIfNotPresent(String clientProfileName) {
        if (!clientProfiles.containsKey(clientProfileName)) {
            clientProfiles.put(clientProfileName, ClientProfile.builder().name(clientProfileName).build());
        }
    }

    private void addAclProfileNotPresent(String aclProfileName) {
        if (!aclProfiles.containsKey(aclProfileName)) {
            aclProfiles.put(aclProfileName, AclProfile.builder().name(aclProfileName).build());
        }
    }

    private void addSubscriptionIfNotPresent(String rawSubscription) {
        if (!subscriptions.containsKey(rawSubscription)) {
            String subscription = rawSubscription;
            boolean exported = true;
            String shareName = "";

            // Check for exported subscriptions
            if (subscription.startsWith("#noexport/")) {
                exported = false;
                subscription = subscription.substring("#noexport/".length());
            }

            // Check for shared subscriptions
            if (subscription.startsWith("#share/")) {
                // Get the share name
                subscription = subscription.substring("#share/".length());
                int firstSlashChar = subscription.indexOf('/');
                shareName = subscription.substring(0, firstSlashChar - 1);
                subscription = subscription.substring(firstSlashChar + 1);
            }
            subscriptions.put(rawSubscription, Subscription.builder()
                    .rawSubscription(rawSubscription)
                    .subscription(subscription)
                    .exported(exported)
                    .sharedName(shareName)
                    .build());
        }
    }

    private void getQueueSubscriptions() throws IOException {
        List<Map<String, Object>> queueResponse = solaceHttpSemp.getQueues();
        for (Map<String, Object> queue : queueResponse) {
            String queueName = (String) queue.get("queueName");
            boolean exclusive = "exclusive".equals(queue.get("accessType"));
            Boolean durable;
            if (solaceHttpSemp.sempVersionIsGreaterThan("2.10")) {
                durable = (Boolean) queue.get("durable");
            } else {
                durable = (Boolean) queue.get("isDurable");
            }
            Queue endpoint = Queue.builder()
                    .name(queueName)
                    .type(ChannelType.QUEUE)
                    .durable(durable)
                    .exclusive(exclusive)
                    .subscriptions(new ArrayList<>())
                    .build();
            channels.put("Q-" + queueName, endpoint);

            // Add topic subscriptions for the queue
            List<Map<String, Object>> queueSubscriptions = solaceHttpSemp.getSubscriptionForQueue(queueName);
            for (Map<String, Object> queueSubscription : queueSubscriptions) {
                String subscriptionTopic = (String) queueSubscription.get("subscriptionTopic");
                addSubscriptionIfNotPresent(subscriptionTopic);
                endpoint.getSubscriptions().add(subscriptions.get(subscriptionTopic));
            }

            // Add the egress flows to the clients
            List<Map<String, Object>> egressFlows = solaceHttpSemp.getClientsForQueueTxFlow(queueName);
            for (Map<String, Object> egressFlow : egressFlows) {
                String clientName = egressFlow.get("clientName").toString();
                Client clientObj = clientNames.get(clientName);
                if (clientObj != null) {
                    // Found the clientName
                    clientObj.getRxEndpoints().add(endpoint);
                }
            }
        }
    }

    @Override
    public void startSubscribers(final BrokerExceptionHandler brokerExceptionHandler) {
        solaceMessageConsumer.configure(this, (SolaceBrokerIdentity) getBrokerIdentity(),
                (SolaceBrokerAuthentication) getBrokerAuthentication(), getDiscoveryOperation().getSubscriptionSet());
        solaceMessageConsumer.createMessageConsumer();
        solaceMessageConsumer.startConsumer();
    }

    @Override
    public NormalizedEvent normalizeMessage(Object vendorSpecificMessage) {
        BytesXMLMessage msg = (BytesXMLMessage) vendorSpecificMessage;
        String topicName = msg.getDestination().getName();
        log.trace("Got solace message on destination {}", topicName);
        BasicNormalizedMessage normalizedMessage = new BasicNormalizedMessage();
        BasicNormalizedEvent normalizedEvent = new BasicNormalizedEvent();
        normalizedEvent.setNormalizedMessage(normalizedMessage);
        normalizedEvent.setChannelName(topicName);
        if (msg.getDestination() instanceof QueueImpl) {
            normalizedEvent.setChannelType("queue");
        } else {
            normalizedEvent.setChannelType("topic");
        }

        if (msg instanceof TextMessage) {
            normalizedMessage.setPayload(((TextMessage) msg).getText().getBytes(Charsets.UTF_8));
        } else {
            normalizedMessage.setPayload(msg.getBytes());
        }

        normalizedMessage.setMessageSize(msg.getAttachmentContentLength() + topicName.length() /*TODO: Plus header length */);
        normalizedMessage.setReceivedTimestamp(msg.getReceiveTimestamp());
        if (msg.getSenderTimestamp() != null) {
            normalizedMessage.setSentTimestamp(msg.getSenderTimestamp());
        }

        return normalizedEvent;
    }

    @Override
    public void stopSubscribers() {
        solaceMessageConsumer.removeSubscriptions();
    }

    @Override
    public void closeSession() {
        log.info("Messages received: {}", messagesReceived);
        log.info("Messages dropped: {}", messagesDropped);
        solaceMessageConsumer.closeSession();
    }

    @Override
    public PluginConfigData getPluginConfiguration() {
        return BasicPluginConfigData.builder()
                .pluginName(PLUGIN_NAME)
                .brokerCapabilities(getBrokerCapabilities())
                .inputSchema(new HashMap<>())
                .brokerAuthenticationClass(SolaceBrokerAuthentication.class)
                .brokerIdentityClass(SolaceBrokerIdentity.class)
                .brokerType(PLUGIN_NAME)
                .pluginClass(getClass())
                .build();
    }

    @Override
    public BrokerCapabilities getBrokerCapabilities() {
        BrokerCapabilities brokerCapabilities = new BrokerCapabilities();
        brokerCapabilities.setCapabilities(new HashSet<>(
                Arrays.asList(BrokerCapabilities.Capability.TIMED_MESSAGE_CONSUMER
                       // , BrokerCapabilities.Capability.NO_SCHEMA_INFERENCE
                )));
        return brokerCapabilities;
    }

}
