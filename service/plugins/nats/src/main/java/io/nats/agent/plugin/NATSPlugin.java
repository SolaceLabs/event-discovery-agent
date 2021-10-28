package io.nats.agent.plugin;
import com.event.discovery.agent.types.event.common.DiscoveryData;
import com.event.discovery.agent.framework.AbstractBrokerPlugin;
import com.event.discovery.agent.framework.VendorBrokerPlugin;
import com.event.discovery.agent.framework.exception.BrokerExceptionHandler;
import com.event.discovery.agent.framework.model.BasicPluginConfigData;
import com.event.discovery.agent.framework.model.BrokerConfigData;
import com.event.discovery.agent.framework.model.NormalizedEvent;
import com.event.discovery.agent.framework.model.PluginConfigData;
import com.event.discovery.agent.framework.model.broker.BrokerCapabilities;
import com.event.discovery.agent.framework.model.broker.NoAuthBrokerAuthentication;
import io.micrometer.core.instrument.MeterRegistry;
import io.nats.agent.plugin.model.NATSIdentity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

@Slf4j
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class NATSPlugin extends AbstractBrokerPlugin implements VendorBrokerPlugin {

    NATSConsumer natsConsumer;

    public NATSPlugin(MeterRegistry meterRegistry) {
        super(meterRegistry);
    }

    @Override
    public PluginConfigData getPluginConfiguration() {
        log.info("getPluginConfiguration()");
        return BasicPluginConfigData.builder()
                .pluginName("NATS")
                .brokerCapabilities(getBrokerCapabilities())
                .inputSchema(new HashMap<>())
                .brokerAuthenticationClass(NoAuthBrokerAuthentication.class)
                .brokerIdentityClass(NATSIdentity.class)
                .brokerType("NATS")
                .pluginClass(getClass())
                .build();
    }

    @Override
    protected BrokerCapabilities getBrokerCapabilities() {
        BrokerCapabilities brokerCapabilities = new BrokerCapabilities();
        Set<BrokerCapabilities.Capability> capabilities = brokerCapabilities.getCapabilities();
        capabilities.add(BrokerCapabilities.Capability.TIMED_MESSAGE_CONSUMER);
        //capabilities.add(BrokerCapabilities.Capability.NO_SCHEMA_INFERENCE);
        return brokerCapabilities;
    }

    @Override
    public BrokerConfigData getBrokerConfig() {
        var identity = (NATSIdentity)getBrokerIdentity();
        var monitoringClient = new MonitoringClient(identity);
        DiscoveryData discoveryData = new DiscoveryData();

        monitoringClient.getConfig(discoveryData);

        return BrokerConfigData.builder()
                .configuration(Collections.EMPTY_MAP)
                .commonDiscoveryData(discoveryData)
                .build();
    }
    
    @Override
    public void startSubscribers(BrokerExceptionHandler brokerExceptionHandler) {
        log.info("startSubscribers()");
        natsConsumer = new NATSConsumer((NATSIdentity) getBrokerIdentity(), getDiscoveryOperation(), this);
        natsConsumer.startSubscribers(brokerExceptionHandler);
    }
    
    @Override
    public NormalizedEvent normalizeMessage(Object vendorSpecificMessage) {
        return natsConsumer.normalizeMessage(vendorSpecificMessage);
    }
    
    @Override
    public void stopSubscribers() {
        log.info("stopSubscribers()");
        natsConsumer.stopSubscribers();
    }

    @Override
    public void closeSession() {
        log.info("closeSession()");
    }
}