package com.event.discovery.agent.solace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.event.discovery.agent.solace.config.SolacePluginProperties;
import com.event.discovery.agent.framework.model.Paging;
import com.event.discovery.agent.framework.model.PluginObjectMapper;
import com.event.discovery.agent.framework.model.SempFlatResponse;
import com.event.discovery.agent.framework.model.SempListResponse;
import com.event.discovery.agent.framework.model.WebClientProperties;
import com.event.discovery.agent.solace.model.broker.SolaceBrokerAuthentication;
import com.event.discovery.agent.solace.model.broker.SolaceBrokerIdentity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
@SuppressWarnings("PMD.NullAssignment")
public class SolaceHttpSemp {
    private WebClient webClient;
    private final ObjectProvider<WebClient> webClientObjectProvider;
    private final ObjectMapper objectMapper;
    private final SolacePluginProperties solaceProperties;

    private SolaceBrokerIdentity brokerIdentity;
    private SolaceBrokerAuthentication brokerAuthentication;
    private String sempVersion = "2.7";

    private final static String GET_SYSTEM_INFORMATION = "/SEMP/v2/config/about/api";
    private final static String GET_QUEUES_URI = "/SEMP/v2/monitor/msgVpns/{msgvpn}/queues";
    private final static String GET_DTES_URI = "/SEMP/v2/monitor/msgVpns/{msgvpn}/topicEndpoints";
    private final static String GET_DTES_TXFLOWS_URI_2_12 = "/SEMP/v2/monitor/msgVpns/{msgvpn}/topicEndpoints/{topicEndpoint}/txFlows";
    private final static String GET_DTES_EGRESS_FLOWS_URI_2_11 = "/SEMP/v2/monitor/msgVpns/{msgvpn}/topicEndpoints/{topicEndpoint}/egressFlows";
    private final static String GET_SUBSCRIPTIONS_FOR_CLIENT_URI = "/SEMP/v2/monitor/msgVpns/{msgvpn}/clients/{clientname}/subscriptions";
    private final static String GET_CLIENTS_URI = "/SEMP/v2/monitor/msgVpns/{msgvpn}/clients";
    private final static String GET_CLIENTUSERNAMES_URI = "/SEMP/v2/monitor/msgVpns/{msgvpn}/clientUsernames";
    private final static String GET_CLIENTS_FOR_QUEUE_EGRESS_FLOWS_URI_2_10 = "/SEMP/v2/monitor/msgVpns/{msgvpn}/queues/{queuename}/egressFlows";
    private final static String GET_CLIENTS_FOR_QUEUE_TX_FLOWS_URI_2_11 = "/SEMP/v2/monitor/msgVpns/{msgvpn}/queues/{queuename}/txFlows";
    private final static String GET_TOPIC_SUBSCRIPTIONS_FOR_QUEUE_URI = "/SEMP/v2/monitor/msgVpns/{msgvpn}/queues/{queuename}/subscriptions";
    private final static String SELECT_CLIENTSUBSCRIPTION_2_11 = "clientSubscription";
    private final static String SELECT_CLIENTSUBSCRIPTION_2_12 = "subscriptionTopic";
    private final static String SELECT_CLIENTNAME = "clientName";
    private final static String SELECT_CLIENTFIELDS_2_10 = "clientName,clientUsername,clientAddress,aclProfile,profile";
    private final static String SELECT_CLIENTFIELDS_2_11 = "clientName,clientUsername,clientAddress,aclProfile,clientProfile";
    private final static String SELECT_CLIENTFIELDS_2_13 = "clientName,clientUsername,clientAddress,aclProfileName,clientProfileName";
    private final static String SELECT_CLIENTUSERNAMEFIELDS = "aclProfileName,clientProfileName,clientUsername";
    private final static String SELECT_SUBSCRIPTIONTOPICS = "subscriptionTopic";
    private final static String SELECT_QUEUENAME_2_11 = "queueName,accessType,durable";
    private final static String SELECT_QUEUENAME_2_10 = "queueName,accessType,isDurable";
    private final static String SELECT_DTES_2_12 = "topicEndpointName,destinationTopic,accessType,durable";
    private final static String SELECT_DTES_2_11 = "topicEndpointName,destination,accessType,isDurable";
    private final static String SELECT_DTES_TXFLOWS = "clientName";

    @Autowired
    public SolaceHttpSemp(ObjectProvider<WebClient> webClientObjectProvider,
                          PluginObjectMapper pluginObjectMapper,
                          SolacePluginProperties solaceProperties) {
        this.webClientObjectProvider = webClientObjectProvider;
        objectMapper = pluginObjectMapper.getObjectMapper();
        this.solaceProperties = solaceProperties;
    }

    public void initialize(SolaceBrokerIdentity brokerIdentity,
                           SolaceBrokerAuthentication brokerAuthentication) {
        this.brokerIdentity = brokerIdentity;
        this.brokerAuthentication = brokerAuthentication;
        createWebClient(brokerAuthentication);
    }

    private void createWebClient(SolaceBrokerAuthentication brokerAuthentication) {
        WebClientProperties webClientProperties = new WebClientProperties();
        if (!StringUtils.isEmpty(brokerAuthentication.getTrustStoreLocation())) {
            webClientProperties.setTrustStoreLocation(brokerAuthentication.getTrustStoreLocation());
        }
        if (!StringUtils.isEmpty(brokerAuthentication.getTrustStorePassword())) {
            webClientProperties.setTrustStorePassword(brokerAuthentication.getTrustStorePassword());
        }
        webClient = webClientObjectProvider.getObject(webClientProperties);
    }

    public void discoverSempVersion() throws IOException {
        Map<String, Object> rawSystemInformation = getResultsFlatFromSemp(GET_SYSTEM_INFORMATION);
        sempVersion = (String) rawSystemInformation.get("sempVersion");
        log.debug("Using semp version {}", sempVersion);
    }

    public List<Map<String, Object>> getQueues() throws IOException {
        return getNonBackwardCompatibleSempResult("2.10",
                GET_QUEUES_URI, SELECT_QUEUENAME_2_11, null,
                GET_QUEUES_URI, SELECT_QUEUENAME_2_10, null);
    }

    public List<Map<String, Object>> getClientSubscriptions(String clientName) throws IOException {

        Map<String, String> substitutionMap = Map.of("clientname", clientName);
        return getNonBackwardCompatibleSempResult("2.11",
                GET_SUBSCRIPTIONS_FOR_CLIENT_URI, SELECT_CLIENTSUBSCRIPTION_2_12, substitutionMap,
                GET_SUBSCRIPTIONS_FOR_CLIENT_URI, SELECT_CLIENTSUBSCRIPTION_2_11, substitutionMap);
    }

    public List<Map<String, Object>> getClientUsernames() throws IOException {
        return getResultsListMapFromSemp(GET_CLIENTUSERNAMES_URI, SELECT_CLIENTUSERNAMEFIELDS);
    }

    public List<Map<String, Object>> getClients() throws IOException {
        if (sempVersionIsGreaterThan("2.12.0")) {
            return getResultsListMapFromSemp(GET_CLIENTS_URI, SELECT_CLIENTFIELDS_2_13, null);
        } else if (sempVersionIsGreaterThan("2.10.0")) {
            return getResultsListMapFromSemp(GET_CLIENTS_URI, SELECT_CLIENTFIELDS_2_11, null);
        }
        return getResultsListMapFromSemp(GET_CLIENTS_URI, SELECT_CLIENTFIELDS_2_10, null);
    }

    public List<Map<String, Object>> getDTEs() throws IOException {
        return getNonBackwardCompatibleSempResult("2.11",
                GET_DTES_URI, SELECT_DTES_2_12, null,
                GET_DTES_URI, SELECT_DTES_2_11, null);
    }

    public List<Map<String, Object>> getDTEsTxFlows(String topicEndpoint) throws IOException {
        return getNonBackwardCompatibleSempResult("2.11",
                GET_DTES_TXFLOWS_URI_2_12, SELECT_DTES_TXFLOWS, Map.of("topicEndpoint", topicEndpoint),
                GET_DTES_EGRESS_FLOWS_URI_2_11, SELECT_DTES_TXFLOWS, Map.of("topicEndpoint", topicEndpoint));
    }

    public List<Map<String, Object>> getSubscriptionForQueue(String queueName) throws IOException {
        return getResultsListMapFromSemp(
                GET_TOPIC_SUBSCRIPTIONS_FOR_QUEUE_URI,
                SELECT_SUBSCRIPTIONTOPICS,
                Map.of("queuename", queueName));
    }

    public List<Map<String, Object>> getClientsForQueueTxFlow(String queueName) throws IOException {
        String txFlowUri = GET_CLIENTS_FOR_QUEUE_EGRESS_FLOWS_URI_2_10;

        if (sempVersionIsGreaterThan("2.10")) {
            txFlowUri = GET_CLIENTS_FOR_QUEUE_TX_FLOWS_URI_2_11;
        }

        return getResultsListMapFromSemp(
                txFlowUri,
                SELECT_CLIENTNAME,
                Map.of("queuename", queueName));
    }

    private List<Map<String, Object>> getResultsListMapFromSemp(String uriPath,
                                                                String selectFields) throws IOException {
        return getResultsListMapFromSemp(uriPath, selectFields, null);
    }

    private Map<String, Object> getResultsFlatFromSemp(String uriPath) throws IOException {
        return getSempFlatRequest(createFlatUriBuilderFunction(uriPath, Collections.emptyMap()));
    }

    private List<Map<String, Object>> getResultsListMapFromSemp(String uriPath,
                                                                String selectFields,
                                                                Map<String, String> additionalSubstitutionFields) throws IOException {
        List<Map<String, Object>> sempObject = new ArrayList<>();
        Map<String, String> substitutionMap = new HashMap<>();
        substitutionMap.put("msgvpn", brokerIdentity.getMessageVpn());
        if (!MapUtils.isEmpty(additionalSubstitutionFields)) {
            substitutionMap.putAll(additionalSubstitutionFields);
        }
        getSempListRequest(sempObject, createUriBuilderFunction(uriPath, selectFields, substitutionMap));
        return sempObject;
    }

    private Function<UriBuilder, URI> createUriBuilderFunction(String uriPath,
                                                               String selectFields,
                                                               Map<String, String> substitutionMap) {
        return (uriBuilder) -> uriBuilder
                .path(uriPath)
                .queryParam("select", selectFields)
                .host(brokerIdentity.getSempHost())
                .port(brokerIdentity.getSempPort())
                .scheme(brokerIdentity.getSempScheme())
                .queryParam("count", solaceProperties.getHttpSempV2PageSize())
                .build(substitutionMap);
    }

    private Function<UriBuilder, URI> createFlatUriBuilderFunction(String uriPath,
                                                                   Map<String, String> substitutionMap) {
        return (uriBuilder) -> uriBuilder
                .path(uriPath)
                .host(brokerIdentity.getSempHost())
                .port(brokerIdentity.getSempPort())
                .scheme(brokerIdentity.getSempScheme())
                .build(substitutionMap);
    }

    private void getSempListRequest(List<Map<String, Object>> list, Function<UriBuilder, URI> uriMethod) throws
            IOException {
        SempListResponse<Map<String, Object>> sempListResponse = getSempListResponse(uriMethod);

        if (sempListResponse != null) {
            if (!CollectionUtils.isEmpty(sempListResponse.getData())) {
                list.addAll(sempListResponse.getData());
            }

            handlePagedSempResponse(list, sempListResponse);
        }
    }

    private void handlePagedSempResponse(List<Map<String, Object>> list,
                                         SempListResponse<Map<String, Object>> sempListResponse) throws com.fasterxml.jackson.core.JsonProcessingException {
        Paging paging = sempListResponse.getMeta().getPaging();
        while (paging != null) {
            log.debug("Paging {}", paging.getNextPageUri());
            final Paging paging2 = paging;
            sempListResponse = getSempListResponse((uriBuilder) -> URI.create(paging2.getNextPageUri()));
            if (sempListResponse != null) {
                if (!CollectionUtils.isEmpty(sempListResponse.getData())) {
                    list.addAll(sempListResponse.getData());
                }
                paging = sempListResponse.getMeta().getPaging();
            } else {
                paging = null;
            }
        }
    }

    private SempListResponse<Map<String, Object>> getSempListResponse(Function<UriBuilder, URI> uriMethod) throws
            com.fasterxml.jackson.core.JsonProcessingException {
        String rawResponse = webClient
                .get()
                .uri(uriMethod)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, buildBasicAuthorization())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return objectMapper.readValue(rawResponse,
                new TypeReference<>() {
                });
    }

    private Map<String, Object> getSempFlatRequest(Function<UriBuilder, URI> uriMethod) throws IOException {
        String rawResponse = webClient
                .get()
                .uri(uriMethod)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, buildBasicAuthorization())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        SempFlatResponse<Map<String, Object>> sempFlatResponse = objectMapper.readValue(rawResponse,
                new TypeReference<>() {
                });

        if (sempFlatResponse != null) {
            return sempFlatResponse.getData();
        }
        return null;
    }

    private String buildBasicAuthorization() {
        return "Basic " +
                Base64.getEncoder().encodeToString(
                        (brokerAuthentication.getSempUsername() +
                                ':' +
                                brokerAuthentication.getSempPassword())
                                .getBytes(StandardCharsets.UTF_8)
                );
    }

    private List<Map<String, Object>> getNonBackwardCompatibleSempResult(String lastVersionForOldSemp,
                                                                         String newSempUri,
                                                                         String newSempSelectors,
                                                                         Map<String, String> newSubstitutionMap,
                                                                         String oldSempUri,
                                                                         String oldSempSelectors,
                                                                         Map<String, String> oldSubstitutionMap) throws IOException {
        if (sempVersionIsGreaterThan(lastVersionForOldSemp)) {
            return getResultsListMapFromSemp(newSempUri, newSempSelectors, newSubstitutionMap);
        }
        return getResultsListMapFromSemp(oldSempUri, oldSempSelectors, oldSubstitutionMap);
    }


    public boolean sempVersionIsGreaterThan(String target) {
        String[] currentSempVersion = sempVersion.split("\\.");
        String[] targetSempVersion = target.split("\\.");

        int versionIndex = 0;
        while (versionIndex < currentSempVersion.length && versionIndex < targetSempVersion.length) {
            if (Integer.parseInt(currentSempVersion[versionIndex]) > Integer.parseInt(targetSempVersion[versionIndex])) {
                return true;
            } else if (Integer.parseInt(currentSempVersion[versionIndex]) < Integer.parseInt(targetSempVersion[versionIndex])) {
                return false;
            }
            versionIndex += 1;
        }
        return false;
    }
}
