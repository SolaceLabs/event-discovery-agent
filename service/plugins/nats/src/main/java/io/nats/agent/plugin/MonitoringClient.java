package io.nats.agent.plugin;

import com.event.discovery.agent.types.event.common.Channel;
import com.event.discovery.agent.types.event.common.ChannelToSubscriptionRelationship;
import com.event.discovery.agent.types.event.common.Client;
import com.event.discovery.agent.types.event.common.ClientToChannelRelationship;
import com.event.discovery.agent.types.event.common.ClientToSubscriptionRelationship;
import com.event.discovery.agent.types.event.common.DiscoveryData;
import com.event.discovery.agent.types.event.common.Subscription;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.event.discovery.agent.framework.utils.IDGenerator;
import io.nats.agent.plugin.model.Connz;
import io.nats.agent.plugin.model.NATSIdentity;
import io.nats.agent.plugin.model.StreamConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonitoringClient {
    private NATSIdentity identity;
    private final HttpClient httpClient;
    private final int LIMIT = 1024;
    ObjectMapper mapper = new ObjectMapper();
    private HashMap<String, String> subMap = new HashMap<>();
    private final IDGenerator idGenerator = new IDGenerator();

    public MonitoringClient(NATSIdentity identity) {
        this.identity = identity;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    public void getConfig(DiscoveryData discoveryData) {
        getSubscriptionConfig(discoveryData);
        getJetStreamConfig(discoveryData);
    }

    private void getSubscriptionConfig(DiscoveryData discoveryData) {
        int offset = 0;
        // TODO: handle paging
        var uri = String.format("%s://%s:%d/connz?subs=1&limit=%d&offset=%d",
                identity.getAdminPortocol(), identity.getHostname(), identity.getAdminPort(), LIMIT, offset);
        HttpRequest request = HttpRequest.newBuilder()
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(uri))
                .header("content-type", "application/json")
                .build();
        HttpResponse<String> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Connz connz = mapper.readValue(response.body(), Connz.class);
            connz.getConnections().forEach(conn -> {
                var commClient = new Client();
                commClient.setId(String.valueOf(conn.getCid()));
                commClient.setName(conn.getName());
                commClient.setType(conn.getType()); // consumer
                discoveryData.getClients().add(commClient);

                conn.getSubscriptions_list().forEach(sub ->{
                    var c2s = new ClientToSubscriptionRelationship();
                    c2s.setClientId(commClient.getId());
                    if (!subMap.containsKey(sub)){
                        subMap.put(sub, idGenerator.generateRandomUniqueId("1"));
                        var commSub = new Subscription();
                        commSub.setId(subMap.get(sub));
                        commSub.setMatchCriteria(sub);
                        discoveryData.getSubscriptions().add(commSub);
                    }
                    c2s.setSubscriptionId(subMap.get(sub));
                    discoveryData.getClientToSubscriptionRelationships().add(c2s);
                });
            });
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private void getJetStreamConfig(DiscoveryData discoveryData) {
        var uri = String.format("%s://%s:%d/jsz?streams=1&config=1",
                identity.getAdminPortocol(), identity.getHostname(), identity.getAdminPort());
        HttpRequest request = HttpRequest.newBuilder()
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(uri))
                .header("content-type", "application/json")
                .build();
        HttpResponse<String> response = null;

        try {
            Map<String, Object> channelType = Map.of("type", "queue");
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            var jsonDocument = Configuration.defaultConfiguration().jsonProvider().parse(response.body());
            Configuration jsonPathConf = Configuration
                    .builder()
                    .mappingProvider(new JacksonMappingProvider())
                    .jsonProvider(new JacksonJsonProvider())
                    .options(Option.SUPPRESS_EXCEPTIONS)
                    .build();

            var fakeClient = new Client();
            fakeClient.setId(idGenerator.generateRandomUniqueId("1"));
            discoveryData.getClients().add(fakeClient);

            var jsonPathCtx = JsonPath.using(jsonPathConf).parse(jsonDocument);
            var typeRef = new TypeRef<List<StreamConfig>>() {};
            var result = jsonPathCtx.read("$.account_details[*].stream_detail[*].config", typeRef);
            result.forEach(c -> {
                Channel commChannel = new Channel();
                commChannel.setId(idGenerator.generateRandomUniqueId("1"));
                commChannel.setName(c.getName());
                commChannel.setAdditionalAttributes(channelType);
                discoveryData.getChannels().add(commChannel);

                var c2c = new ClientToChannelRelationship();
                c2c.setClientId(fakeClient.getId());
                c2c.setChannelId(commChannel.getId());
                discoveryData.getClientToChannelRelationships().add(c2c);

                c.getSubjects().forEach(sub ->{
                    var c2s = new ChannelToSubscriptionRelationship();
                    c2s.setChannelId(commChannel.getId());
                    if (!subMap.containsKey(sub)){
                        subMap.put(sub, idGenerator.generateRandomUniqueId("1"));
                        var commSub = new Subscription();
                        commSub.setId(subMap.get(sub));
                        commSub.setMatchCriteria(sub);
                        discoveryData.getSubscriptions().add(commSub);
                    }
                    c2s.setSubscriptionId(subMap.get(sub));
                    discoveryData.getChannelToSubscriptionRelationships().add(c2s);
                });
            });
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
