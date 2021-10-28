package com.hivemq.agent.plugin;

import com.hivemq.agent.plugin.model.HiveMQIdentity;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.event.discovery.agent.framework.AbstractBrokerPlugin;
import com.event.discovery.agent.framework.exception.DiscoverySupportCode;
import com.event.discovery.agent.framework.exception.EventDiscoveryAgentException;
import com.event.discovery.agent.framework.model.DiscoveryOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
public class HiveMQRuntimeListener {
    private Mqtt3AsyncClient client;
    private DiscoveryOperation discoveryOperation;
    private AbstractBrokerPlugin plugin;

    public void configure(HiveMQIdentity identity,
                          DiscoveryOperation discoveryOperation,
                          AbstractBrokerPlugin plugin) {
        this.discoveryOperation = discoveryOperation;
        this.plugin = plugin;
        client = MqttClient.builder()
                .useMqttVersion3()
                .identifier(UUID.randomUUID().toString())
                .serverHost(identity.getHostname())
                .serverPort(identity.getClientPort())
                .buildAsync();
    }

    public void start() {
        client.connect()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101,
                                "Unable to connect to HiveMQ broker", (Exception) throwable);
                    } else {
                        subscribe();
                    }
                });
    }

    public void subscribe() {
        // Get the first subscription in the list (short-cut)
        String subscription = discoveryOperation.getSubscriptionSet().iterator().next();
        log.info("Subscribing to {}", subscription);
        client.subscribeWith()
                .topicFilter(subscription)
                .callback(plugin::queueReceivedMessage)
                .send();

    }

    public void finish() {
        client.disconnect();
    }
}
