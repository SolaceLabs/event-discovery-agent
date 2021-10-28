package com.hivemq.agent.plugin;

import com.hivemq.agent.plugin.model.HiveMQClientList;
import com.hivemq.agent.plugin.model.HiveMQIdentity;
import com.hivemq.agent.plugin.model.HiveMQSubscriptionList;
import com.event.discovery.agent.framework.model.WebClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class HiveMQAdmin {
    private WebClient webClient;
    private HiveMQIdentity identity;
    private final ObjectProvider<WebClient> webClientObjectProvider;

    @Autowired
    public HiveMQAdmin(ObjectProvider<WebClient> webClientObjectProvider) {
        this.webClientObjectProvider = webClientObjectProvider;
    }

    public void initialize(HiveMQIdentity hiveMQIdentity) {
        identity = hiveMQIdentity;
        WebClientProperties webClientProperties = new WebClientProperties();
        webClient = webClientObjectProvider.getObject(webClientProperties);
    }

    public HiveMQClientList getClientList() {
        HiveMQClientList rawResponse = webClient
                .get()
                .uri("http://" + identity.getHostname() + ":" + identity.getAdminPort() +
                        "/api/v1/mqtt/clients")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(HiveMQClientList.class)
                .block();
        return rawResponse;
    }

    public HiveMQSubscriptionList getSubscriptionList(String clientId) {
        HiveMQSubscriptionList rawResponse = webClient
                .get()
                .uri("http://" + identity.getHostname() + ":" + identity.getAdminPort() +
                        "/api/v1/mqtt/clients/" + clientId + "/subscriptions")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(HiveMQSubscriptionList.class)
                .block();
        return rawResponse;
    }
}
