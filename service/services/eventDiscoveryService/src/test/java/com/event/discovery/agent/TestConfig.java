package com.event.discovery.agent;

import com.event.discovery.agent.apps.model.PluginCollectionBO;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@SuppressWarnings("PMD.CouplingBetweenObjects")
@TestConfiguration
@Profile("OFFLINETEST")
public class TestConfig {

    private AdminClient adminClient = mock(AdminClient.class);

    private Properties adminClientProperties = new Properties();

    @Bean
    @Primary
    public KafkaConsumer getKafkaConsumer() {
        return mock(KafkaConsumer.class);
    }

    @Bean
    @Primary
    public WebClient getWebClient() {
        WebClient mockWebClient = mock(WebClient.class);

        // Fix the bean post processor
        WebClient.Builder mockWebClientBuilder = mock(WebClient.Builder.class);
        when(mockWebClient.mutate()).thenReturn(mockWebClientBuilder);
        when(mockWebClientBuilder.filters(any())).thenReturn(mockWebClientBuilder);
        when(mockWebClientBuilder.build()).thenReturn(mockWebClient);

        return mockWebClient;
    }

    @Bean
    @Primary
    public PluginCollectionBO pluginCollectionBO() {
        return new PluginCollectionBO();
    }

    @Bean
    @Primary
    @Scope(SCOPE_PROTOTYPE)
    public AdminClient getAdminClient(Properties properties) {
        adminClientProperties.putAll(properties);
        return adminClient;
    }

    @Bean
    @Qualifier("testAdminClientProperties")
    public Properties getAdminClientProperties() {
        return adminClientProperties;
    }

    @Primary
    @Bean
    public JCSMPProperties getJcsmpProperties() {
        return mock(JCSMPProperties.class);
    }

    @Primary
    @Bean
    public JCSMPSession getJcsmpSession() {
        return mock(JCSMPSession.class);
    }

}

