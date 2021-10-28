package com.event.discovery.agent.integrationTests.offline.kafkaCommon.kafka;

import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaAuthentication;
import com.event.discovery.agent.kafkaCommon.model.ApacheKafkaIdentity;
import com.event.discovery.agent.rest.model.v1.DiscoveryOperationRequestDTO;
import com.event.discovery.agent.framework.model.DiscoveryOperation;
import com.event.discovery.agent.framework.model.ServiceAuthentication;
import com.event.discovery.agent.framework.model.ServiceIdentity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class KafkaConfigRequests {

    protected DiscoveryOperationRequestDTO createKafkaSASLPlainSSLEventDiscoveryRequest(int duration) {
        DiscoveryOperationRequestDTO request = new DiscoveryOperationRequestDTO();


        ApacheKafkaIdentity identity = buildApacheKafkaIdentity();
        ServiceAuthentication connectorAuth = new ServiceAuthentication();
        connectorAuth.setBasicAuthPassword("ImSoBasic");
        connectorAuth.setBasicAuthUsername("BasicUser");
        connectorAuth.setKeyStoreLocation("here");
        connectorAuth.setKeyStorePassword("mypass");
        connectorAuth.setTrustStoreLocation("there");
        connectorAuth.setTrustStorePassword("trustypassword");

        ApacheKafkaAuthentication auth = ApacheKafkaAuthentication.builder()
                .authenticationType(ApacheKafkaAuthentication.KafkaAuthentiationType.SASL_PLAIN)
                .transportType(ApacheKafkaAuthentication.KafkaTransportType.SSL)
                .trustStoreLocation("a/b/c/d")
                .trustStorePassword("qwerqwer")
                .keyStoreLocation("/e/f/c/h")
                .keyStorePassword("gdgsgsd")
                .keyPassword("rewrwer")
                .adminUsername("admin123")
                .adminPassword("adminpass")
                .consumerUsername("consumer123")
                .consumerPassword("consumerpass")
                .brokerType("KAFKA")
                .connector(connectorAuth)
                .build();

        DiscoveryOperation discoveryOperation = buildDiscoveryOperation(duration);
        request.setBrokerIdentity(identity);
        request.setBrokerAuthentication(auth);
        request.setDiscoveryOperation(discoveryOperation);
        return request;
    }

    protected DiscoveryOperationRequestDTO createKafkaSASLSCRAM256SSLEventDiscoveryRequest(int duration) {
        return createKafkaSASLSCRAMSSLEventDiscoveryRequest(ApacheKafkaAuthentication.KafkaAuthentiationType.SASL_SCRAM_256, duration);
    }

    protected DiscoveryOperationRequestDTO createKafkaSASLSCRAM512SSLEventDiscoveryRequest(int duration) {
        return createKafkaSASLSCRAMSSLEventDiscoveryRequest(ApacheKafkaAuthentication.KafkaAuthentiationType.SASL_SCRAM_512, duration);
    }

    protected DiscoveryOperationRequestDTO createKafkaSASLSCRAMSSLEventDiscoveryRequest(
            ApacheKafkaAuthentication.KafkaAuthentiationType authType, int duration) {
        DiscoveryOperationRequestDTO request = new DiscoveryOperationRequestDTO();

        ApacheKafkaIdentity identity = buildApacheKafkaIdentity();

        ApacheKafkaAuthentication auth = ApacheKafkaAuthentication.builder()
                .authenticationType(authType)
                .transportType(ApacheKafkaAuthentication.KafkaTransportType.SSL)
                .trustStoreLocation("a/b/c/d")
                .trustStorePassword("qwerqwer")
                .keyStoreLocation("/e/f/c/h")
                .keyStorePassword("gdgsgsd")
                .keyPassword("rewrwer")
                .consumerUsername("kafkaclient")
                .consumerPassword("password123")
                .adminUsername("kafkaclient2")
                .adminPassword("password1234")
                .brokerType("KAFKA")
                .build();

        DiscoveryOperation discoveryOperation = buildDiscoveryOperation(duration);
        request.setBrokerIdentity(identity);
        request.setBrokerAuthentication(auth);
        request.setDiscoveryOperation(discoveryOperation);
        return request;
    }

    protected DiscoveryOperationRequestDTO createKafkaSASLGSSAPISSLEventDiscoveryRequest(int duration) {
        DiscoveryOperationRequestDTO request = new DiscoveryOperationRequestDTO();


        ApacheKafkaIdentity identity = buildApacheKafkaIdentity();

        ApacheKafkaAuthentication auth = ApacheKafkaAuthentication.builder()
                .authenticationType(ApacheKafkaAuthentication.KafkaAuthentiationType.SASL_GSSAPI)
                .transportType(ApacheKafkaAuthentication.KafkaTransportType.SSL)
                .trustStoreLocation("a/b/c/d")
                .trustStorePassword("qwerqwer")
                .keyStoreLocation("/e/f/c/h")
                .keyStorePassword("gdgsgsd")
                .keyPassword("rewrwer")
                .principal("saslconsumer/confluent.mymaas.net@TEST.CONFLUENT.IO")
                .keyTabLocation("/Users/user/confluentDemo/5.5/saslconsumer.keytab")
                .krb5ConfigurationLocation("/Users/user/confluentDemo/5.5/krb.conf")
                .serviceName("myService1")
                .brokerType("KAFKA")
                .build();

        DiscoveryOperation discoveryOperation = buildDiscoveryOperation(duration);
        request.setBrokerIdentity(identity);
        request.setBrokerAuthentication(auth);
        request.setDiscoveryOperation(discoveryOperation);
        return request;
    }

    private ApacheKafkaIdentity buildApacheKafkaIdentity() {
        ServiceIdentity serviceIdentity = new ServiceIdentity();
        serviceIdentity.setHostname("connectors");
        serviceIdentity.setPort(9095);

        return ApacheKafkaIdentity.builder()
                .host("someHost")
                .brokerType("KAFKA")
                .connector(serviceIdentity)
                .build();
    }

    private DiscoveryOperation buildDiscoveryOperation(int duration) {
        return DiscoveryOperation.noArgsBuilder()
                .messageQueueLength(100000)
                .operationType("eventDiscovery")
                .subscriptionSet(new HashSet<>(Arrays.asList("ATopic")))
                .durationInSecs(duration)
                .build();
    }

    protected DiscoveryOperationRequestDTO createDefaultKafkaEventDiscoveryRequest() {

        return createKafkaEventDiscoveryRequest(2);
    }

    protected DiscoveryOperationRequestDTO createKafkaEventDiscoveryRequest(int duration) {

        DiscoveryOperationRequestDTO request = new DiscoveryOperationRequestDTO();


        ApacheKafkaIdentity identity = buildApacheKafkaIdentity();

        ServiceAuthentication connectorAuth = new ServiceAuthentication();
        connectorAuth.setBasicAuthPassword("ImSoBasic");
        connectorAuth.setBasicAuthUsername("BasicUser");
        connectorAuth.setKeyStoreLocation("here");
        connectorAuth.setKeyStorePassword("mypass");
        connectorAuth.setTrustStoreLocation("there");
        connectorAuth.setTrustStorePassword("trustypassword");

        ApacheKafkaAuthentication auth = ApacheKafkaAuthentication.builder()
                .authenticationType(ApacheKafkaAuthentication.KafkaAuthentiationType.NOAUTH)
                .transportType(ApacheKafkaAuthentication.KafkaTransportType.PLAINTEXT)
                .brokerType("KAFKA")
                .connector(connectorAuth)
                .build();

        DiscoveryOperation discoveryOperation = DiscoveryOperation.noArgsBuilder()
                .messageQueueLength(100000)
                .operationType("eventDiscovery")
                //.subscriptionSet(new HashSet<>(Arrays.asList("aTopicName")))
                .subscriptionSet(Collections.EMPTY_SET)
                .durationInSecs(duration)
                .build();
        request.setBrokerIdentity(identity);
        request.setBrokerAuthentication(auth);
        request.setDiscoveryOperation(discoveryOperation);
        return request;
    }

}
