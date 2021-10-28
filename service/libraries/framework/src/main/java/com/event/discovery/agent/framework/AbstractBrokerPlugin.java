package com.event.discovery.agent.framework;

import com.event.discovery.agent.framework.utils.StoppableRunnable;
import com.event.discovery.agent.framework.model.BasicPluginConfigData;
import com.event.discovery.agent.framework.model.DiscoveryOperation;
import com.event.discovery.agent.framework.model.PluginConfigData;
import com.event.discovery.agent.framework.model.broker.BasicBrokerIdentity;
import com.event.discovery.agent.framework.model.broker.BrokerAuthentication;
import com.event.discovery.agent.framework.model.broker.BrokerCapabilities;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;
import com.event.discovery.agent.framework.model.broker.NoAuthBrokerAuthentication;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Data
public abstract class AbstractBrokerPlugin<T> implements BrokerPlugin {

    private BrokerIdentity brokerIdentity;
    private BrokerAuthentication brokerAuthentication;
    private DiscoveryOperation discoveryOperation;

    private LinkedBlockingQueue<T> receivedMessageQueue;

    protected int messagesReceived;
    protected int messagesDropped;

    private MeterRegistry meterRegistry;

    private ExecutorService executorService;
    private StoppableRunnable messageNormalizer;

    private long startOperationTs;
    private long endOperationTs;

    public AbstractBrokerPlugin(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    protected abstract void closeSession();

    @Override
    public void initialize(BrokerIdentity brokerIdentity, BrokerAuthentication brokerAuthentication, DiscoveryOperation discoveryOperation) {
        this.brokerAuthentication = brokerAuthentication;
        this.brokerIdentity = brokerIdentity;
        this.discoveryOperation = discoveryOperation;
        receivedMessageQueue = new LinkedBlockingQueue<>(discoveryOperation.getMessageQueueLength());
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public LinkedBlockingQueue<?> getReceivedMessageQueue() {
        return receivedMessageQueue;
    }

    public void queueReceivedMessage(T vendorSpecificMessage) {
        if (!receivedMessageQueue.offer(vendorSpecificMessage)) {
            messagesDropped++;
            meterRegistry.counter("eventDiscovery.brokerconnector.droppedMessages").increment();
        } else {
            messagesReceived++;
            meterRegistry.counter("eventDiscovery.brokerconnector.receivedMessages").increment();
        }
    }

    // Provide an initial implementation of broker capabilities
    protected BrokerCapabilities getBrokerCapabilities() {
        BrokerCapabilities brokerCapabilities = new BrokerCapabilities();
        brokerCapabilities.setCapabilities(Collections.EMPTY_SET);
        return brokerCapabilities;
    }

    public PluginConfigData getPluginConfiguration() {
        return BasicPluginConfigData.builder()
                .pluginName("A plugin has no name")
                .brokerCapabilities(getBrokerCapabilities())
                .inputSchema(new HashMap<>())
                .brokerIdentityClass(BasicBrokerIdentity.class)
                .brokerAuthenticationClass(NoAuthBrokerAuthentication.class)
                .brokerType("NONE")
                .build();
    }

    public boolean hasCapability(BrokerCapabilities.Capability capability) {
        return getBrokerCapabilities().getCapabilities().contains(capability);
    }

}
