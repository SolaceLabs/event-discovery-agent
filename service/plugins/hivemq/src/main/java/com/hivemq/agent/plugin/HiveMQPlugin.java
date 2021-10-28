package com.hivemq.agent.plugin;

import com.hivemq.agent.plugin.model.HiveMQClientList;
import com.hivemq.agent.plugin.model.HiveMQIdentity;
import com.hivemq.agent.plugin.model.HiveMQSubscriptionList;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.event.discovery.agent.types.event.common.Client;
import com.event.discovery.agent.types.event.common.ClientToSubscriptionRelationship;
import com.event.discovery.agent.types.event.common.DiscoveryData;
import com.event.discovery.agent.types.event.common.Subscription;
import com.event.discovery.agent.framework.AbstractBrokerPlugin;
import com.event.discovery.agent.framework.VendorBrokerPlugin;
import com.event.discovery.agent.framework.exception.BrokerExceptionHandler;
import com.event.discovery.agent.framework.model.BasicPluginConfigData;
import com.event.discovery.agent.framework.model.BrokerConfigData;
import com.event.discovery.agent.framework.model.NormalizedEvent;
import com.event.discovery.agent.framework.model.PluginConfigData;
import com.event.discovery.agent.framework.model.broker.BrokerCapabilities;
import com.event.discovery.agent.framework.model.broker.NoAuthBrokerAuthentication;
import com.event.discovery.agent.framework.model.messages.BasicNormalizedEvent;
import com.event.discovery.agent.framework.model.messages.BasicNormalizedMessage;
import com.event.discovery.agent.framework.utils.IDGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class HiveMQPlugin extends AbstractBrokerPlugin implements VendorBrokerPlugin {

    private final HiveMQAdmin hiveMQAdmin;
    private final IDGenerator idGenerator;
    private final HiveMQRuntimeListener listener;

    @Autowired
    public HiveMQPlugin(MeterRegistry meterRegistry, HiveMQAdmin hiveMQAdmin, IDGenerator idGenerator,
                        HiveMQRuntimeListener listener) {
        super(meterRegistry);
        this.hiveMQAdmin = hiveMQAdmin;
        this.idGenerator = idGenerator;
        this.listener = listener;
    }

    @Override
    public BrokerConfigData getBrokerConfig() {
        hiveMQAdmin.initialize((HiveMQIdentity) getBrokerIdentity());

        // Configure the listener
        listener.configure((HiveMQIdentity) getBrokerIdentity(), getDiscoveryOperation(),
                this);

        DiscoveryData discoveryData = new DiscoveryData();

        // Get Clients
        HiveMQClientList clientList = hiveMQAdmin.getClientList();
        List<Client> clients = clientList.getItems().stream()
                .map(c -> {
                    Client client = new Client();
                    client.setId(idGenerator.generateRandomUniqueId("1"));
                    client.setName(c.getId());
                    client.setType("");
                    return client;
                })
                .collect(Collectors.toList());
        discoveryData.getClients().addAll(clients);
        log.info("Adding clients {}", clients);

        // Get subscriptions
        clients.stream()
                .forEach(client -> getSubscriptions(client, discoveryData));
        return BrokerConfigData.builder()
                .configuration(Collections.EMPTY_MAP)
                .commonDiscoveryData(discoveryData)
                .build();
    }

    private void getSubscriptions(Client client, DiscoveryData discoveryData) {
        HiveMQSubscriptionList subList = hiveMQAdmin.getSubscriptionList(client.getName());
        subList.getItems().stream()
                .forEach(sub -> {
                    // Add subscription to discovery data
                    Subscription newSub = new Subscription();
                    newSub.setId(idGenerator.generateRandomUniqueId("1"));
                    newSub.setMatchCriteria(sub.getTopicFilter());
                    discoveryData.getSubscriptions().add(newSub);

                    // Add client to subscription relationship
                    ClientToSubscriptionRelationship clientSub = new ClientToSubscriptionRelationship();
                    clientSub.setClientId(client.getId());
                    clientSub.setSubscriptionId(newSub.getId());
                    discoveryData.getClientToSubscriptionRelationships().add(clientSub);
                });
    }

    @Override
    public void startSubscribers(BrokerExceptionHandler brokerExceptionHandler) {
        listener.start();
    }

    @Override
    public NormalizedEvent normalizeMessage(Object vendorSpecificMessage) {
        if (vendorSpecificMessage instanceof Mqtt3Publish) {
            Mqtt3Publish mqttMessage = (Mqtt3Publish) vendorSpecificMessage;
            if (mqttMessage.getPayload().isPresent()) {
                ByteBuffer message = mqttMessage.getPayload().get();

                BasicNormalizedEvent event = new BasicNormalizedEvent();
                BasicNormalizedMessage msg = new BasicNormalizedMessage();
                event.setNormalizedMessage(msg);

                // Set the topic name
                event.setChannelName(mqttMessage.getTopic().toString());
                event.setChannelType("topic");

                // Copy the payload
                byte[] bytes = new byte[message.remaining()];
                message.get(bytes);

                msg.setPayload(bytes);
                return event;
            }
        }
        return null;
    }

    @Override
    public void stopSubscribers() {
        listener.finish();
    }

    @Override
    public void closeSession() {

    }

    @Override
    public PluginConfigData getPluginConfiguration() {
        return BasicPluginConfigData.builder()
                .pluginName("HiveMQ")
                .brokerCapabilities(getBrokerCapabilities())
                .inputSchema(new HashMap<>())
                .brokerAuthenticationClass(NoAuthBrokerAuthentication.class)
                .brokerIdentityClass(HiveMQIdentity.class)
                .brokerType("HIVEMQ")
                .pluginClass(getClass())
                .build();
    }

    @Override
    protected BrokerCapabilities getBrokerCapabilities() {
        BrokerCapabilities brokerCapabilities = new BrokerCapabilities();
        Set<BrokerCapabilities.Capability> capabilities = brokerCapabilities.getCapabilities();
        capabilities.add(BrokerCapabilities.Capability.TIMED_MESSAGE_CONSUMER);
        return brokerCapabilities;
    }

}
