package com.event.discovery.agent.kafkaApache;

import com.event.discovery.agent.framework.model.BasicPluginConfigData;
import com.event.discovery.agent.framework.model.PluginConfigData;
import com.event.discovery.agent.kafkaCommon.KafkaCommon;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaAuthentication;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaIdentity;
import com.event.discovery.agent.kafkaCommon.plugin.AbstractApacheKafkaPlugin;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
public class ApacheKafkaPlugin extends AbstractApacheKafkaPlugin {

    @Autowired
    public ApacheKafkaPlugin(ObjectProvider<KafkaConsumer<byte[], byte[]>> kafkaConsumerObjectProvider,
                             MeterRegistry meterRegistry, KafkaCommon kafkaCommon) {
        super(kafkaConsumerObjectProvider, meterRegistry, kafkaCommon);
    }

    @Override
    protected String getPluginName() {
        return "KAFKA";
    }

    @Override
    public PluginConfigData getPluginConfiguration() {
        return BasicPluginConfigData.builder()
                .pluginName(getPluginName())
                .brokerCapabilities(getBrokerCapabilities())
                .inputSchema(new HashMap<>())
                .brokerAuthenticationClass(ApacheKafkaAuthentication.class)
                .brokerIdentityClass(ApacheKafkaIdentity.class)
                .brokerType("KAFKA")
                .pluginClass(getClass())
                .build();
    }

}
