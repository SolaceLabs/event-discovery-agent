package com.event.discovery.agent.solace;

import com.event.discovery.agent.framework.exception.AuthorizationException;
import com.event.discovery.agent.framework.exception.DiscoverySupportCode;
import com.event.discovery.agent.framework.exception.EventDiscoveryAgentException;
import com.event.discovery.agent.solace.model.broker.SolaceBrokerAuthentication;
import com.event.discovery.agent.solace.model.broker.SolaceBrokerIdentity;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Set;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
public class SolaceMessageConsumer {
    private final ObjectProvider<JCSMPSession> jcsmpSessionObjectProvider;
    private JCSMPSession session;
    private SolacePlugin solacePlugin;
    private SolaceBrokerIdentity brokerIdentity;
    private SolaceBrokerAuthentication brokerAuthentication;
    private Set<String> subscriptions;
    private XMLMessageConsumer consumer;


    public SolaceMessageConsumer(ObjectProvider<JCSMPSession> jcsmpSessionObjectProvider) {
        this.jcsmpSessionObjectProvider = jcsmpSessionObjectProvider;
    }

    public void configure(SolacePlugin solacePlugin, SolaceBrokerIdentity brokerIdentity,
                          SolaceBrokerAuthentication brokerAuthentication, Set<String> subscriptions) {
        this.solacePlugin = solacePlugin;
        this.brokerIdentity = brokerIdentity;
        this.brokerAuthentication = brokerAuthentication;
        this.subscriptions = subscriptions;

        session = jcsmpSessionObjectProvider.getObject(configureJcsmpProperties());

        try {
            session.connect();
        } catch (JCSMPException e) {
            log.error("Unable to connect to broker {}", brokerIdentity.getClientHost(), e);
            if (e.getMessage().contains("The username or password is incorrect")) {
                throw new AuthorizationException(e.getMessage(), "Broker authentication failed with provided credentials");
            }
            throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101, "Unable to connect to broker", e);
        }
    }

    public void createMessageConsumer() {
        try {
            consumer = session.getMessageConsumer(new XMLMessageListener() {
                @Override
                public void onReceive(BytesXMLMessage msg) {
                    solacePlugin.queueReceivedMessage(msg);
                }

                @Override
                public void onException(JCSMPException e) {
                    log.error("Consumer received exception", e);
                    throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101, "Exception while consuming messages", e);
                }
            });
        } catch (JCSMPException e) {
            log.error("Message Consumer exception", e);
            throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101, "Exception while consuming messages", e);
        }
    }

    public void startConsumer() {
        try {
            consumer.start();
            for (String subscription : subscriptions) {
                session.addSubscription(JCSMPFactory.onlyInstance().createTopic(subscription));
            }
        } catch (JCSMPException e) {
            log.error("Unable to connect to broker", e);
            throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101, "Unable to connect to broker", e);
        }

        log.debug("Connected. Awaiting message...");
    }

    public void removeSubscriptions() {
        //remove the subscriptions to stop messages, but keep the consumer alive to finish receiving all messages
        if (!session.isClosed()) {
            try {
                for (String subscription : subscriptions) {
                    session.removeSubscription(JCSMPFactory.onlyInstance().createTopic(subscription));

                }
            } catch (JCSMPException e) {
                log.error("Unable to remove subscriptions", e);
                throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101, "Unable to remove subscriptions", e);
            }
        }
    }

    public void closeSession() {
        if (consumer != null) {
            consumer.close();
        }
        if (session != null) {
            session.closeSession();
        }
    }

    private JCSMPProperties configureJcsmpProperties() {
        final JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST,
                brokerIdentity.getClientProtocol() + "://" + brokerIdentity.getClientHost() + ":" + brokerIdentity.getMessagingPort());

        properties.setProperty(JCSMPProperties.VPN_NAME, brokerIdentity.getMessageVpn()); // message-vpn

        // Basic authentication
        if (!StringUtils.isEmpty(brokerAuthentication.getClientUsername())) {
            properties.setProperty(JCSMPProperties.USERNAME, brokerAuthentication.getClientUsername()); // client-username
            properties.setProperty(JCSMPProperties.AUTHENTICATION_SCHEME, JCSMPProperties.AUTHENTICATION_SCHEME_BASIC);
        }
        if (!StringUtils.isEmpty(brokerAuthentication.getClientPassword())) {
            properties.setProperty(JCSMPProperties.PASSWORD, brokerAuthentication.getClientPassword()); // client-password
        }
        properties.setProperty(JCSMPProperties.GENERATE_RCV_TIMESTAMPS, true);
        properties.setProperty(JCSMPProperties.MESSAGE_CALLBACK_ON_REACTOR, true);  // don't need the extra buffer
        JCSMPChannelProperties cp = new JCSMPChannelProperties();
        cp.setReconnectRetries(-1);

        properties.setProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES, cp);

        // Custom truststore
        if (!StringUtils.isEmpty(brokerAuthentication.getTrustStoreLocation())) {
            properties.setProperty(JCSMPProperties.SSL_TRUST_STORE, brokerAuthentication.getTrustStoreLocation());
        }
        if (!StringUtils.isEmpty(brokerAuthentication.getTrustStorePassword())) {
            properties.setProperty(JCSMPProperties.SSL_TRUST_STORE_PASSWORD, brokerAuthentication.getTrustStorePassword());
        }

        // Client cert authentication
        if (!StringUtils.isEmpty(brokerAuthentication.getKeyStoreLocation())) {
            properties.setProperty(JCSMPProperties.SSL_KEY_STORE, brokerAuthentication.getKeyStoreLocation());
            properties.setProperty(JCSMPProperties.AUTHENTICATION_SCHEME, JCSMPProperties.AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);
        }
        if (!StringUtils.isEmpty(brokerAuthentication.getKeyStorePassword())) {
            properties.setProperty(JCSMPProperties.SSL_KEY_STORE_PASSWORD, brokerAuthentication.getKeyStorePassword());
        }
        if (!StringUtils.isEmpty(brokerAuthentication.getKeyPassword())) {
            properties.setProperty(JCSMPProperties.SSL_PRIVATE_KEY_PASSWORD, brokerAuthentication.getKeyPassword());
        }


        return properties;
    }
}
