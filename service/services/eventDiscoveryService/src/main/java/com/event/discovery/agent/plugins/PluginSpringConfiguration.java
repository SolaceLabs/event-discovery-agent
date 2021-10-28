package com.event.discovery.agent.plugins;

import com.event.discovery.agent.apps.model.PluginCollectionBO;
import com.event.discovery.agent.plugins.broker.BrokerAuthenticationDeserializer;
import com.event.discovery.agent.plugins.broker.BrokerIdentityDeserializer;
import com.event.discovery.agent.framework.model.WebClientProperties;
import com.event.discovery.agent.framework.model.broker.BrokerAuthentication;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;
import com.event.discovery.agent.google.GooglePubSubSubscriber;
import com.event.discovery.agent.google.GooglePubSubSubscriberImpl;
import com.event.discovery.agent.google.factories.SubscriptionAdminClientFactory;
import com.event.discovery.agent.google.factories.TopicAdminClientFactory;
import com.solacesystems.jcsmp.InvalidPropertiesException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.ResourceUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Slf4j
@Configuration
public class PluginSpringConfiguration {

    @Bean
    public TopicAdminClientFactory createTopicAdminClientFactory() {
        return new TopicAdminClientFactory();
    }

    @Bean
    public SubscriptionAdminClientFactory createSubscriptionAdminClientFactory() {
        return new SubscriptionAdminClientFactory();
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public GooglePubSubSubscriber createGooglePubSubSubscriber() {
        return new GooglePubSubSubscriberImpl();
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public AdminClient getAdminClient(Properties properties) {
        return KafkaAdminClient.create(properties);
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public WebClient getWebClient(WebClientProperties webClientProperties) {

        // This list is make up of the default ciphers, plus TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256 which
        // is required for the confluent schema registry REST API.
        List<String> supportedCiphers = Collections.unmodifiableList(
                Arrays.asList("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA", "TLS_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA"));
        if (!StringUtils.isEmpty(webClientProperties.getTrustStoreLocation())) {
            String trustStoreLocation = webClientProperties.getTrustStoreLocation();
            String trustStorePassword = webClientProperties.getTrustStorePassword();

            try {
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(new FileInputStream(ResourceUtils.getFile(trustStoreLocation)), trustStorePassword.toCharArray());
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
                SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
                        .ciphers(supportedCiphers, SupportedCipherSuiteFilter.INSTANCE)
                        .trustManager(tmf);

                if (!StringUtils.isEmpty(webClientProperties.getKeyStoreLocation())) {
                    String keyStoreLocation = webClientProperties.getKeyStoreLocation();
                    String keyStorePassword = webClientProperties.getKeyStorePassword();

                    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    keyStore.load(new FileInputStream(ResourceUtils.getFile(keyStoreLocation)), keyStorePassword.toCharArray());
                    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    kmf.init(keyStore, keyStorePassword.toCharArray());

                    sslContextBuilder.keyManager(kmf);
                }

                SslContext sslContext = sslContextBuilder.build();
                HttpClient httpClient = HttpClient.create()
                        .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));
                ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
                return WebClient.builder().clientConnector(connector).build();
            } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | UnrecoverableKeyException e) {
                throw new RuntimeException(e);
            }
        }
        return WebClient.builder().build();

    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public KafkaConsumer getKafkaConsumer(Properties kafkaConsumerProperties) {
        return new KafkaConsumer(kafkaConsumerProperties);
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public JCSMPSession getJcsmpSession(JCSMPProperties jcsmpProperties) {
        try {
            return JCSMPFactory.onlyInstance().createSession(jcsmpProperties);
        } catch (InvalidPropertiesException e) {
            throw new BeanCreationException("JCSMPSession", "Failed to create JCSMP session", e);
        }
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public PluginCollectionBO pluginCollectionBO() {
        return new PluginCollectionBO();
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    @Primary
    public Validator getValidator() {
        return new Validator() {
            @Override
            public boolean supports(Class<?> aClass) {
                return false;
            }

            @Override
            public void validate(Object o, Errors errors) {
            }
        };
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer addBrokerDeserialization(BrokerAuthenticationDeserializer brokerAuthenticationDeserializer,
                                                                          BrokerIdentityDeserializer brokerIdentityDeserializer) {
        return jacksonObjectMapperBuilder -> {
            jacksonObjectMapperBuilder.deserializerByType(BrokerAuthentication.class, brokerAuthenticationDeserializer);
            jacksonObjectMapperBuilder.deserializerByType(BrokerIdentity.class, brokerIdentityDeserializer);
        };
    }

    @Bean
    public BrokerAuthenticationDeserializer getBrokerAuthenticationDeserializer(PluginConfigurationMap pluginConfigurationMap) {
        return new BrokerAuthenticationDeserializer(pluginConfigurationMap);
    }

    @Bean
    public BrokerIdentityDeserializer getBrokerIdentityDeserializer(PluginConfigurationMap pluginConfigurationMap) {
        return new BrokerIdentityDeserializer(pluginConfigurationMap);
    }
}
