package com.event.discovery.agent.integrationTests.offline.solace;

import com.event.discovery.agent.apps.model.EventDiscoveryOperationDiscoveryResultBO;
import com.event.discovery.agent.solace.model.broker.SolaceBrokerAuthentication;
import com.event.discovery.agent.solace.model.broker.SolaceBrokerIdentity;
import com.event.discovery.agent.types.event.common.DiscoveryData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.event.discovery.agent.integrationTests.offline.BaseEventDiscoveryAgentOfflineAnnotatedIT;
import com.event.discovery.agent.rest.model.v1.DiscoveryOperationRequestDTO;
import com.event.discovery.agent.rest.model.v1.DiscoveryOperationResponseDTO;
import com.event.discovery.agent.types.event.common.Broker;
import com.event.discovery.agent.types.event.common.Channel;
import com.event.discovery.agent.types.event.common.ChannelToSubscriptionRelationship;
import com.event.discovery.agent.types.event.common.Client;
import com.event.discovery.agent.types.event.common.ClientToChannelRelationship;
import com.event.discovery.agent.types.event.common.ClientToSubscriptionRelationship;
import com.event.discovery.agent.types.event.common.Subscription;
import com.event.discovery.agent.framework.model.DiscoveryOperation;
import com.event.discovery.agent.framework.model.JobStatus;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.MapMessage;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.NcssCount")
public class SolaceEventDiscoveryTestsIT extends BaseEventDiscoveryAgentOfflineAnnotatedIT {
    @Autowired
    protected WebClient webClient;

    @Captor
    private ArgumentCaptor<Function<UriBuilder, URI>> uriFunctionCaptor;

    @Autowired
    private ResourceLoader resourceLoader;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();

    private final Map<String, String> requestResponseMap = buildRequestResponseMap();

    @Autowired
    private ObjectProvider<JCSMPSession> jcsmpSessionObjectProvider;

    @Before
    public void before() throws JCSMPException {
        mockJcsmp();
        mockHttp();
    }

    private void mockJcsmp() throws JCSMPException {
        JCSMPSession jcsmpSession = jcsmpSessionObjectProvider.getObject(new JCSMPProperties());
        final ArgumentCaptor<XMLMessageListener> messageListenerArg = ArgumentCaptor.forClass(XMLMessageListener.class);
        when(jcsmpSession.getMessageConsumer(messageListenerArg.capture())).thenAnswer(
                (Answer) invocation -> setupMessageConsumer(messageListenerArg.getValue())
        );
    }

    private XMLMessageConsumer setupMessageConsumer(XMLMessageListener messageListener) throws JCSMPException {
        XMLMessageConsumer messageConsumer = mock(XMLMessageConsumer.class);
        doAnswer((i) -> {

            // Send a test message
            TextMessage nullMessage = mock(TextMessage.class);
            Destination dest1 = mock(Destination.class);
            TextMessage textMessage = mock(TextMessage.class);
            when(textMessage.getDestination()).thenReturn(dest1);
            when(textMessage.getText()).thenReturn("{ \"greg1\" : \"someval\"}");
            messageListener.onReceive(textMessage);

            // Send a map message
            MapMessage mapMessage = mock(MapMessage.class);
            Destination dest2 = mock(Destination.class);
            when(dest2.getName()).thenReturn("SomeTopic2");
            when(mapMessage.getDestination()).thenReturn(dest2);
            byte[] bytes = {01, 02, 03, 04, 05, 118, 101, 114};
            when(mapMessage.getBytes()).thenReturn(bytes);
            messageListener.onReceive(mapMessage);

            // Send a null text message
            when(dest1.getName()).thenReturn("SomeTopic");
            when(nullMessage.getDestination()).thenReturn(dest1);
            when(nullMessage.getText()).thenReturn(null);
            messageListener.onReceive(nullMessage);

            return null;
        }).when(messageConsumer).start();
        return messageConsumer;
    }

    private void mockHttp() {
        WebClient.RequestHeadersUriSpec request = mock(WebClient.RequestHeadersUriSpec.class);
        when(webClient.get()).thenReturn(request);

        when(request.uri(uriFunctionCaptor.capture())).thenAnswer(
                (Answer) invocation -> {
                    URI newUri = uriFunctionCaptor.getValue().apply(defaultUriBuilderFactory.builder());
                    return createWebClientResponse(requestResponseMap.get(newUri.toString()));
                }
        );
    }

    private WebClient.RequestHeadersSpec createWebClientResponse(String response) {
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(any(MediaType.class))).thenReturn(requestHeadersSpec);

        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        Mono monoSubjectList = mock(Mono.class);

        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(monoSubjectList);
        when(monoSubjectList.block()).thenReturn(response);

        return requestHeadersSpec;
    }

    private Map<String, String> buildRequestResponseMap() {
        Map<String, String> requestResponseResult = new HashMap<>();
        try {
            Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                    .getResources("classpath:solacePlugin/*.json");
            for (Resource r : resources) {
                String requestResponseRaw = new BufferedReader(new InputStreamReader(r.getInputStream()))
                        .lines().collect(Collectors.joining("\n"));
                RequestResponse requestResponse = objectMapper.readValue(requestResponseRaw, RequestResponse.class);
                requestResponseResult.put(requestResponse.getRequest().asText(), requestResponse.getResponse().toString());
            }
        } catch (IOException e) {
            fail("Could not find request/response files");
        }
        return requestResponseResult;
    }

    @Test
    public void testSolaceEventDiscovery() {
        DiscoveryOperationResponseDTO response = createEventListenerOperation(
                createEventDiscoveryRequest(3));

        // Validate obfuscation in response
        String obfuscatedPassword = "**************";
        assertEquals("eventDiscovery", response.getDiscoveryOperation().getOperationType());
        assertEquals("sempUser", ((SolaceBrokerAuthentication) response.getBrokerAuthentication()).getSempUsername());
        assertEquals(obfuscatedPassword, ((SolaceBrokerAuthentication) response.getBrokerAuthentication()).getSempPassword());
        assertEquals("clientUser", ((SolaceBrokerAuthentication) response.getBrokerAuthentication()).getClientUsername());
        assertEquals(obfuscatedPassword, ((SolaceBrokerAuthentication) response.getBrokerAuthentication()).getClientPassword());
        assertEquals(obfuscatedPassword, ((SolaceBrokerAuthentication) response.getBrokerAuthentication()).getTrustStorePassword());
        assertEquals(obfuscatedPassword, ((SolaceBrokerAuthentication) response.getBrokerAuthentication()).getKeyStorePassword());
        assertEquals(obfuscatedPassword, ((SolaceBrokerAuthentication) response.getBrokerAuthentication()).getKeyPassword());

        final String jobId = response.getJobId();
        assertNotNull(jobId);

        waitUntilJobStatus(JobStatus.COMPLETED, jobId);

        String asyncApiResult = getDiscoveryOperationAsyncAPIResult(jobId);

        // No real async api validation for now since this area of the code is immature and
        // is expected to iterate pretty quickly
        assertNotNull(asyncApiResult);

        EventDiscoveryOperationDiscoveryResultBO result = getResultInInternalModel();

        // Validate obfuscation in result
        Map<String, Object> brokerAuthentication = (Map<String, Object>) result.getPluginInputs().get("brokerAuthentication");
        assertEquals("clientUser", brokerAuthentication.get("clientUsername"));
        assertEquals("clientPass", brokerAuthentication.get("clientPassword"));
        assertEquals("sempUser", brokerAuthentication.get("sempUsername"));
        assertEquals("sempPass", brokerAuthentication.get("sempPassword"));
        assertEquals("secret", brokerAuthentication.get("trustStorePassword"));
        assertEquals("secret1", brokerAuthentication.get("keyStorePassword"));
        assertEquals("secret2", brokerAuthentication.get("keyPassword"));

        Map<String, Object> brokerIdentity = (Map<String, Object>) result.getPluginInputs().get("brokerIdentity");
        assertEquals("solacehost", brokerIdentity.get("sempHost"));
        assertEquals("solacehost", brokerIdentity.get("clientHost"));

        DiscoveryData discoveryData = (DiscoveryData) result.getEvents();

        List<Client> clients = discoveryData.getClients();
        assertEquals(2, clients.size());
        assertThat(clients.stream().map(Client::getName).collect(Collectors.toList()),
                containsInAnyOrder("gregmeldrum-2727.local/75057/#00230001/f9YkNyFR7F", "solclientjs/chrome-83.0.4103-OSX-10.13.6/1495049266/0001"));
        assertThat(clients.stream().map(Client::getType).collect(Collectors.toList()),
                contains("consumer", "consumer"));

        List<Channel> channels = discoveryData.getChannels();
        assertEquals(5, channels.size());
        Channel temporaryQueue = channels.stream()
                .filter(c -> "gregsNewQueue".equals(c.getName()))
                .findFirst().get();
        assertTrue((boolean) temporaryQueue.getAdditionalAttributes().get("exclusive"));
        assertFalse((boolean) temporaryQueue.getAdditionalAttributes().get("durable"));

        Channel durableQueue = channels.stream()
                .filter(c -> "greg1".equals(c.getName()) && "queue".equals(c.getAdditionalAttributes().get("type")))
                .findFirst().get();
        assertTrue((boolean) durableQueue.getAdditionalAttributes().get("exclusive"));
        assertTrue((boolean) durableQueue.getAdditionalAttributes().get("durable"));

        Channel topic = channels.stream()
                .filter(c -> "SomeTopic".equals(c.getName()))
                .findFirst().get();
        assertEquals("topic", topic.getAdditionalAttributes().get("type"));

        Channel tempTE = channels.stream()
                .filter(c -> "greg1".equals(c.getName()) && "topic_endpoint".equals(c.getAdditionalAttributes().get("type")))
                .findFirst().get();
        assertTrue((boolean) tempTE.getAdditionalAttributes().get("exclusive"));
        assertFalse((boolean) tempTE.getAdditionalAttributes().get("durable"));

        Channel dte = channels.stream()
                .filter(c -> "gregsDurableTopic".equals(c.getName()))
                .findFirst().get();
        assertTrue((boolean) dte.getAdditionalAttributes().get("exclusive"));
        assertTrue((boolean) dte.getAdditionalAttributes().get("durable"));

        List<Subscription> subscriptions = discoveryData.getSubscriptions();
        assertEquals(8, subscriptions.size());

        Broker myBroker = discoveryData.getBroker();
        assertEquals("solace", myBroker.getBrokerType());
        assertEquals("myBrokerAlias", myBroker.getBrokerName());
        assertEquals("myvpn", myBroker.getAdditionalAttributes().get("msgvpn"));


        // Validate the clientToChannelRelationships
        List<ClientToChannelRelationship> clientToChannelRelationships = discoveryData.getClientToChannelRelationships();
        Client javaClient = clients.stream()
                .filter(c -> "gregmeldrum-2727.local/75057/#00230001/f9YkNyFR7F".equals(c.getName()))
                .findFirst().get();
        Client jsClient = clients.stream()
                .filter(c -> "solclientjs/chrome-83.0.4103-OSX-10.13.6/1495049266/0001".equals(c.getName()))
                .findFirst().get();

        List<ClientToChannelRelationship> javaClientRelationship = clientToChannelRelationships.stream()
                .filter(rel -> javaClient.getId().equals(rel.getClientId()))
                .collect(Collectors.toList());
        assertEquals(2, javaClientRelationship.size());
        List<String> javaClientChannelIds = javaClientRelationship.stream()
                .map(rel -> rel.getChannelId()).collect(Collectors.toList());
        assertTrue(temporaryQueue.getId(), javaClientChannelIds.contains(temporaryQueue.getId()) &&
                javaClientChannelIds.contains(durableQueue.getId()));


        ClientToChannelRelationship jsClientRelationship = clientToChannelRelationships.stream()
                .filter(rel -> jsClient.getId().equals(rel.getClientId())).findFirst().get();
        assertEquals(dte.getId(), jsClientRelationship.getChannelId());

        List<ClientToSubscriptionRelationship> clientToSubscriptionRelationships = discoveryData.getClientToSubscriptionRelationships();

        // Make sure the java client subscriptions match
        Subscription javaLocalSub = subscriptions.stream()
                .filter(s ->
                        "#P2P/v:single-aws-us-east-1b-1k9czsylgsnj/Dr2lNqxz/gregmeldrum-2727.local/75057/#00230001/f9YkNyFR7F/>".equals(s.getMatchCriteria()))
                .findFirst().get();
        ClientToSubscriptionRelationship javaClientSubRel = clientToSubscriptionRelationships.stream()
                .filter(sub -> javaClient.getId().equals(sub.getClientId()))
                .findFirst().get();
        assertEquals(javaLocalSub.getId(), javaClientSubRel.getSubscriptionId());

        // Make sure the js client subscriptions match
        List<Subscription> jsClientSubs = subscriptions.stream()
                .filter(s -> {
                    String matchCriteria = s.getMatchCriteria();
                    return ">".equals(matchCriteria) || "acme/>".equals(matchCriteria)
                            || "#P2P/v:single-aws-us-east-1b-1k9czsylgsnj/Dr2lNqxz/solclientjs/chrome-83.0.4103-OSX-10.13.6/1495049266/0001/>"
                            .equals(matchCriteria);
                })
                .collect(Collectors.toList());
        for (Subscription sub : jsClientSubs) {
            ClientToSubscriptionRelationship jsClientSubRel = clientToSubscriptionRelationships.stream()
                    .filter(subRel -> sub.getId().equals(subRel.getSubscriptionId()))
                    .findFirst().get();
            assertEquals(jsClient.getId(), jsClientSubRel.getClientId());
        }

        // Validate the shared subscription
        Subscription sharedSubscription = jsClientSubs.stream()
                .filter(s -> "acme/>".equals(s.getMatchCriteria()))
                .findFirst().get();
        assertEquals("shareId", sharedSubscription.getAdditionalAttributes().get("shareName"));
        assertEquals(false, sharedSubscription.getAdditionalAttributes().get("exported"));

        // Validate the queue/dte subscriptions
        List<ChannelToSubscriptionRelationship> channelToSubscriptionRelationships = discoveryData.getChannelToSubscriptionRelationships();
        Subscription sub1 = subscriptions.stream()
                .filter(s -> "another/subscription".equals(s.getMatchCriteria()))
                .findFirst().get();
        Subscription sub2 = subscriptions.stream()
                .filter(s -> "newTopics1".equals(s.getMatchCriteria()))
                .findFirst().get();
        Subscription sub3 = subscriptions.stream()
                .filter(s -> "gregsTopic/*/1".equals(s.getMatchCriteria()))
                .findFirst().get();

        ChannelToSubscriptionRelationship rel1 = channelToSubscriptionRelationships.stream()
                .filter(rel -> sub1.getId().equals(rel.getSubscriptionId()))
                .findFirst().get();
        assertEquals(temporaryQueue.getId(), rel1.getChannelId());

        ChannelToSubscriptionRelationship rel2 = channelToSubscriptionRelationships.stream()
                .filter(rel -> sub2.getId().equals(rel.getSubscriptionId()))
                .findFirst().get();
        assertEquals(temporaryQueue.getId(), rel2.getChannelId());

        ChannelToSubscriptionRelationship rel3 = channelToSubscriptionRelationships.stream()
                .filter(rel -> sub3.getId().equals(rel.getSubscriptionId()))
                .findFirst().get();
        assertEquals(dte.getId(), rel3.getChannelId());
    }


    protected DiscoveryOperationRequestDTO createEventDiscoveryRequest(int duration) {

        DiscoveryOperationRequestDTO request = new DiscoveryOperationRequestDTO();


        SolaceBrokerIdentity identity = SolaceBrokerIdentity.builder()
                .brokerType("SOLACE")
                .clientHost("solacehost")
                .clientProtocol("tcps")
                .messagingPort("9099")
                .messageVpn("myvpn")
                .sempHost("solacehost")
                .sempPort("943")
                .sempScheme("https")
                .brokerAlias("myBrokerAlias")
                .build();

        request.setBrokerIdentity(identity);

        SolaceBrokerAuthentication auth = SolaceBrokerAuthentication.builder()
                .brokerType("SOLACE")
                .clientUsername("clientUser")
                .clientPassword("clientPass")
                .sempUsername("sempUser")
                .sempPassword("sempPass")
                .trustStorePassword("secret")
                .keyStorePassword("secret1")
                .keyPassword("secret2")
                .build();

        request.setBrokerAuthentication(auth);
        request.setDiscoveryOperation(buildDiscoveryOperation(duration));
        return request;
    }

    private DiscoveryOperation buildDiscoveryOperation(int duration) {
        return DiscoveryOperation.noArgsBuilder()
                .messageQueueLength(100000)
                .operationType("eventDiscovery")
                .subscriptionSet(Collections.singleton(">"))
                .durationInSecs(duration)
                .build();
    }

    @Data
    public class UriHolder {
        private String uri;
    }
}
